package com.chopstickshq.quillcore.csr.client;

import com.chopstickshq.quillcore.csr.CsrFgWeights;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL30;

import java.util.concurrent.locks.LockSupport;

/**
 * CSR Frame Gen on the GPU. Per real frame N (final image incl. HUD, just before the swap):
 * copy N into the history ring, estimate flow between N-1 and N (simplified block match),
 * run netA (1/4 res) and netB (1/2 res), compose interp(N-1, N) at t = 0.5 straight into the
 * default framebuffer, swap it, pace, then put N back into the back buffer for the vanilla swap.
 */
final class CsrFrameGen implements AutoCloseable {
	static final float T = 0.5f;
	/** Mean |dLuma| over the frame above which the pair is treated as a scene cut. */
	static final float CUT_THRESHOLD = 0.15f;
	private static final float[] LUMA = {0.2126f, 0.7152f, 0.0722f};

	private final CsrFgWeights weights;
	private final CsrPass pyr;
	private final CsrPass search;
	private final CsrPass packA;
	private final CsrPass packB;
	private final CsrPass compose;
	private final CsrPass conv;
	private final CsrConvNet netA;
	private final CsrConvNet netB;
	private final int vao;

	private int w;
	private int h;
	private int qw, qh, ew, eh, sw, sh, hw, hh;
	private final int[] hist = new int[2];
	private final int[] histFbo = new int[2];
	private int cur;
	private boolean havePrev;
	private int worldTex, worldFbo;
	private boolean worldValid;
	private int pyr4, pyr8, pyr16, pyr4Fbo, pyr8Fbo, pyr16Fbo;
	private final int[] flow16 = new int[2], flow8 = new int[2], flow4 = new int[2];
	private int flow16Fbo, flow8Fbo, flow4Fbo;
	private final int[] packATex = new int[2];
	private final int[] packBTex = new int[3];
	private int packAFbo, packBFbo;
	private int mainFbo, mainFboTex;
	private int[] lastNetA;
	private int[] lastNetB;

	CsrFrameGen(CsrFgWeights weights) {
		this.weights = weights;
		CsrGl.Saved saved = CsrGl.Saved.capture();
		try {
			pyr = new CsrPass("fg_pyr.fsh");
			search = new CsrPass("fg_search.fsh");
			packA = new CsrPass("fg_packa.fsh");
			packB = new CsrPass("fg_packb.fsh");
			compose = new CsrPass("fg_compose.fsh");
			conv = new CsrPass("conv.fsh");
			netA = new CsrConvNet(weights.netA());
			netB = new CsrConvNet(weights.netB());
			vao = GlStateManager._glGenVertexArrays();
			CsrGl.check("CSR Frame Gen init");
		} finally {
			saved.restore();
		}
	}

	private static int half(int v) {
		return Math.max(1, (v + 1) / 2);
	}

	private void ensure(int width, int height) {
		if (hist[0] != 0 && width == w && height == h) {
			return;
		}
		releaseTargets();
		w = width;
		h = height;
		qw = Math.max(1, (w + 3) / 4);
		qh = Math.max(1, (h + 3) / 4);
		ew = half(qw);
		eh = half(qh);
		sw = half(ew);
		sh = half(eh);
		hw = half(w);
		hh = half(h);
		for (int i = 0; i < 2; i++) {
			hist[i] = CsrGl.colorTexture(w, h);
			histFbo[i] = CsrGl.fbo(hist[i]);
		}
		worldTex = CsrGl.colorTexture(w, h);
		worldFbo = CsrGl.fbo(worldTex);
		pyr4 = CsrGl.featureTexture(qw, qh);
		pyr8 = CsrGl.featureTexture(ew, eh);
		pyr16 = CsrGl.featureTexture(sw, sh);
		CsrGl.enableMips(pyr16);
		pyr4Fbo = CsrGl.fbo(pyr4);
		pyr8Fbo = CsrGl.fbo(pyr8);
		pyr16Fbo = CsrGl.fbo(pyr16);
		for (int i = 0; i < 2; i++) {
			flow16[i] = CsrGl.featureTexture(sw, sh);
			flow8[i] = CsrGl.featureTexture(ew, eh);
			flow4[i] = CsrGl.featureTexture(qw, qh);
		}
		flow16Fbo = CsrGl.fbo(flow16);
		flow8Fbo = CsrGl.fbo(flow8);
		flow4Fbo = CsrGl.fbo(flow4);
		for (int i = 0; i < packATex.length; i++) {
			packATex[i] = CsrGl.featureTexture(qw, qh);
		}
		packAFbo = CsrGl.fbo(packATex);
		for (int i = 0; i < packBTex.length; i++) {
			packBTex[i] = CsrGl.featureTexture(hw, hh);
		}
		packBFbo = CsrGl.fbo(packBTex);
		netA.ensure(qw, qh);
		netB.ensure(hw, hh);
		havePrev = false;
		worldValid = false;
	}

	private int mainFbo(RenderTarget main) {
		int tex = ((GlTexture) main.getColorTexture()).glId();
		if (mainFbo == 0 || mainFboTex != tex) {
			CsrGl.deleteFbos(mainFbo);
			mainFbo = CsrGl.fbo(tex);
			mainFboTex = tex;
		}
		return mainFbo;
	}

	/** Forget history (resize, pause, world change) so no pair spans a discontinuity. */
	void reset() {
		havePrev = false;
		worldValid = false;
	}

	/** After world + post, before GUI: keep the HUD-free image for the HUD mask. */
	void captureWorld(RenderTarget main) {
		CsrGl.Saved saved = CsrGl.Saved.capture();
		try {
			CsrGl.drainErrors();
			ensure(main.width, main.height);
			CsrGl.blit(mainFbo(main), w, h, worldFbo, w, h, false);
			CsrGl.check("CSR Frame Gen world capture");
			worldValid = true;
		} finally {
			saved.restore();
		}
	}

	/**
	 * Called with the finished real frame in {@code main}, before the vanilla swap.
	 * @return true if an interpolated frame was presented
	 */
	boolean present(RenderTarget main, long window, int fbW, int fbH, long gapNanos) {
		CsrGl.Saved saved = CsrGl.Saved.capture();
		boolean swapped = false;
		try {
			CsrGl.drainErrors();
			CsrGl.neutralState();
			if (main.width != fbW || main.height != fbH) {
				reset();
				return false;
			}
			ensure(fbW, fbH);
			int src = mainFbo(main);
			int next = cur ^ 1;
			CsrGl.blit(src, w, h, histFbo[next], w, h, false);
			if (!worldValid) {
				// No pre-GUI capture this frame: HUD mask is empty rather than stale.
				CsrGl.blit(src, w, h, worldFbo, w, h, false);
			}
			int prev = hist[cur];
			int curr = hist[next];
			boolean pair = havePrev;
			cur = next;
			havePrev = true;
			worldValid = false;
			if (!pair) {
				CsrGl.check("CSR Frame Gen history");
				return false;
			}

			runFlowAndNets(prev, curr);
			composeInto(0, prev, curr);
			CsrGl.check("CSR Frame Gen passes");

			long shown = System.nanoTime();
			GLFW.glfwSwapBuffers(window);
			swapped = true;
			pace(shown, gapNanos);
			// The back buffer is undefined after a swap: put the real frame back for the vanilla present.
			CsrGl.blit(histFbo[cur], w, h, 0, fbW, fbH, false);
			CsrGl.check("CSR Frame Gen present");
			return true;
		} catch (RuntimeException e) {
			if (swapped) {
				try {
					CsrGl.blit(mainFbo(main), w, h, 0, fbW, fbH, false);
				} catch (RuntimeException ignored) {
					// best effort; the caller disables the feature
				}
			}
			throw e;
		} finally {
			saved.restore();
		}
	}

	private static void pace(long shownAt, long gapNanos) {
		if (gapNanos <= 0) {
			return;
		}
		long until = shownAt + gapNanos;
		long now;
		while ((now = System.nanoTime()) < until) {
			LockSupport.parkNanos(Math.min(until - now, 500_000L));
		}
	}

	private void runFlowAndNets(int prev, int curr) {
		// Luma pyramid: 1/4 (box 4x4 from colour), 1/8, 1/16 + mip chain for the frame mean.
		pyr.use().i("uMode", 0).tex("uPrev", 0, prev, 0).tex("uCurr", 1, curr, 0).tex("uSrc", 2, prev, 0)
				.i("uSrcSize", w, h).f("uLuma", LUMA[0], LUMA[1], LUMA[2]);
		CsrGl.draw(pyr4Fbo, qw, qh, vao);
		pyr.i("uMode", 1).tex("uSrc", 2, pyr4, 0).i("uSrcSize", qw, qh);
		CsrGl.draw(pyr8Fbo, ew, eh, vao);
		pyr.tex("uSrc", 2, pyr8, 0).i("uSrcSize", ew, eh);
		CsrGl.draw(pyr16Fbo, sw, sh, vao);
		CsrGl.generateMips(pyr16);

		// Hierarchical block match, coarse to fine.
		search.use().tex("uPyr", 0, pyr16, 0).i("uSize", sw, sh)
				.tex("uCoarseB", 1, pyr8, 0).tex("uCoarseF", 2, pyr8, 0).i("uCoarseSize", 1, 1)
				.i("uHasCoarse", 0).i("uRadius", 4);
		CsrGl.draw(flow16Fbo, sw, sh, vao);
		search.tex("uPyr", 0, pyr8, 0).i("uSize", ew, eh)
				.tex("uCoarseB", 1, flow16[0], 0).tex("uCoarseF", 2, flow16[1], 0).i("uCoarseSize", sw, sh)
				.i("uHasCoarse", 1).i("uRadius", 1);
		CsrGl.draw(flow8Fbo, ew, eh, vao);
		search.tex("uPyr", 0, pyr4, 0).i("uSize", qw, qh)
				.tex("uCoarseB", 1, flow8[0], 0).tex("uCoarseF", 2, flow8[1], 0).i("uCoarseSize", ew, eh);
		CsrGl.draw(flow4Fbo, qw, qh, vao);

		packA.use().tex("uPyr4", 0, pyr4, 0).tex("uBwd", 1, flow4[0], 0).tex("uFwd", 2, flow4[1], 0)
				.tex("uCurrFinal", 3, curr, 0).tex("uCurrWorld", 4, worldTex, 0)
				.i("uQSize", qw, qh).f("uFlowScale", weights.flowScale());
		common(packA);
		CsrGl.draw(packAFbo, qw, qh, vao);

		int[] a = netA.run(conv, vao, packATex);

		packB.use().tex("uPrev", 0, prev, 0).tex("uCurr", 1, curr, 0).i("uCurrFinal", 1)
				.tex("uPackA0", 2, packATex[0], 0).tex("uPackA1", 3, packATex[1], 0)
				.tex("uNetA0", 4, a[0], 0).tex("uNetA1", 5, a[1], 0).tex("uCurrWorld", 6, worldTex, 0)
				.f("uHSize", hw, hh).f("uT", T).f("uFlowScale", weights.flowScale());
		common(packB);
		CsrGl.draw(packBFbo, hw, hh, vao);

		lastNetA = a;
		lastNetB = netB.run(conv, vao, packBTex);
	}

	private void composeInto(int fbo, int prev, int curr) {
		int[] b = lastNetB;
		compose.use().tex("uPrev", 0, prev, 0).tex("uCurr", 1, curr, 0).i("uCurrFinal", 1)
				.tex("uPackA0", 2, packATex[0], 0).tex("uNetA0", 3, lastNetA[0], 0)
				.tex("uNetB0", 4, b[0], 0).tex("uNetB1", 5, b[1], 0)
				.tex("uPyr16", 6, pyr16, 0).tex("uCurrWorld", 7, worldTex, 0)
				.f("uCutLod", (float) Math.floor(Math.log(Math.max(sw, sh)) / Math.log(2)))
				.f("uCutThresh", CUT_THRESHOLD).f("uT", T)
				.f("uMaskA", weights.maskA()).f("uMaskB", weights.maskB())
				.i("uLpSigmoid", weights.lpSigmoid() ? 1 : 0).f("uFlowScale", weights.flowScale());
		common(compose);
		CsrGl.draw(fbo, w, h, vao);
	}

	private void common(CsrPass p) {
		p.f("uLuma", LUMA[0], LUMA[1], LUMA[2]).f("uFullSizeF", w, h).i("uFullSize", w, h);
	}

	private void releaseTargets() {
		CsrGl.deleteFbos(histFbo[0], histFbo[1], worldFbo, pyr4Fbo, pyr8Fbo, pyr16Fbo,
				flow16Fbo, flow8Fbo, flow4Fbo, packAFbo, packBFbo, mainFbo);
		CsrGl.deleteTextures(hist[0], hist[1], worldTex, pyr4, pyr8, pyr16,
				flow16[0], flow16[1], flow8[0], flow8[1], flow4[0], flow4[1],
				packATex[0], packATex[1], packBTex[0], packBTex[1], packBTex[2]);
		hist[0] = hist[1] = histFbo[0] = histFbo[1] = 0;
		mainFbo = mainFboTex = 0;
		havePrev = false;
	}

	@Override
	public void close() {
		CsrGl.Saved saved = CsrGl.Saved.capture();
		try {
			releaseTargets();
			netA.close();
			netB.close();
			pyr.close();
			search.close();
			packA.close();
			packB.close();
			compose.close();
			conv.close();
			GL30.glDeleteVertexArrays(vao);
		} finally {
			saved.restore();
		}
	}
}
