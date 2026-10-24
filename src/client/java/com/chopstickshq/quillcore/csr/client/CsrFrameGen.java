package com.chopstickshq.quillcore.csr.client;

import com.chopstickshq.quillcore.csr.CsrFgWeights;
import com.chopstickshq.quillcore.csr.CsrMotion;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.concurrent.locks.LockSupport;

/** CSR Frame Gen on the GPU. */
final class CsrFrameGen implements AutoCloseable {
	static final float T = 0.5f;
	/** Mean |dLuma| over the frame above which the pair is treated as a scene cut. */
	static final float CUT_THRESHOLD = 0.15f;
	private static final float[] LUMA = {0.2126f, 0.7152f, 0.0722f};

	private final CsrFgWeights weights;
	private final CsrPass pyr;
	private final CsrPass meLuma, gmGrid, gmCost, gmPick, meBlock, meP8, meProp, meP4, meP2, meOccl;
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
	// Motion search (FgV2): per-frame luma pyramids, then per-tile vectors at (tw, th).
	private int tw, th, bw, bh;
	private final int[][] lum = new int[2][CsrMotion.LEVELS];
	private final int[][] lumFbo = new int[2][CsrMotion.LEVELS];
	private final int[] lw = new int[CsrMotion.LEVELS], lh = new int[CsrMotion.LEVELS];
	private int lumCur;
	private int gmGridTex, gmCostTex, gmTex, gmGridFbo, gmCostFbo, gmFbo;
	private int blockTex, tv8, tv8s, tv4, occlTex, blockFbo, tv8Fbo, tv8sFbo, tv4Fbo, occlFbo;
	/** [0] backward (curr -> prev), [1] forward; each ping-pongs so last frame's result seeds this one. */
	private final int[][] mv = new int[2][2];
	private final int[][] mvFbo = new int[2][2];
	private int mvCur;
	private boolean mvHistValid;
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
			meLuma = new CsrPass("me_luma.fsh");
			gmGrid = new CsrPass("gm_grid.fsh");
			gmCost = new CsrPass("gm_cost.fsh");
			gmPick = new CsrPass("gm_pick.fsh");
			meBlock = new CsrPass("me_block.fsh");
			meP8 = new CsrPass("me_p8.fsh");
			meProp = new CsrPass("me_prop.fsh");
			meP4 = new CsrPass("me_p4.fsh");
			meP2 = new CsrPass("me_p2.fsh");
			meOccl = new CsrPass("me_occl.fsh");
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
		ensureMotion();
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

	private void ensureMotion() {
		lw[0] = w;
		lh[0] = h;
		for (int l = 1; l < CsrMotion.LEVELS; l++) {
			lw[l] = CsrMotion.halve(lw[l - 1]);
			lh[l] = CsrMotion.halve(lh[l - 1]);
		}
		for (int f = 0; f < 2; f++) {
			for (int l = 0; l < CsrMotion.LEVELS; l++) {
				lum[f][l] = CsrGl.texture(lw[l], lh[l], GL30.GL_R16F, GL11.GL_RED, GL30.GL_HALF_FLOAT, true);
				lumFbo[f][l] = CsrGl.fbo(lum[f][l]);
			}
		}
		tw = CsrMotion.tiles(w);
		th = CsrMotion.tiles(h);
		bw = CsrMotion.blocks(tw);
		bh = CsrMotion.blocks(th);
		gmGridTex = floatTexture(64, 36);
		gmCostTex = floatTexture(25, 13);
		gmTex = floatTexture(1, 1);
		gmGridFbo = CsrGl.fbo(gmGridTex);
		gmCostFbo = CsrGl.fbo(gmCostTex);
		gmFbo = CsrGl.fbo(gmTex);
		blockTex = CsrGl.featureTexture(bw, bh);
		tv8 = CsrGl.featureTexture(tw, th);
		tv8s = CsrGl.featureTexture(tw, th);
		tv4 = CsrGl.featureTexture(tw, th);
		occlTex = CsrGl.featureTexture(tw, th);
		blockFbo = CsrGl.fbo(blockTex);
		tv8Fbo = CsrGl.fbo(tv8);
		tv8sFbo = CsrGl.fbo(tv8s);
		tv4Fbo = CsrGl.fbo(tv4);
		occlFbo = CsrGl.fbo(occlTex);
		for (int d = 0; d < 2; d++) {
			for (int i = 0; i < 2; i++) {
				mv[d][i] = CsrGl.featureTexture(tw, th);
				mvFbo[d][i] = CsrGl.fbo(mv[d][i]);
			}
		}
		mvHistValid = false;
	}

	/** RGBA32F: the global-motion costs use 1e9 as "no overlap", which half floats cannot hold. */
	private static int floatTexture(int width, int height) {
		return CsrGl.texture(width, height, GL30.GL_RGBA32F, GL11.GL_RGBA, GL11.GL_FLOAT, false);
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
		mvHistValid = false;
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
			lumCur ^= 1;
			buildLuma(curr);
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

	/** FG2_Luma + FG2_Halve for the new frame into this frame's pyramid. */
	private void buildLuma(int color) {
		CsrTimers.begin("fg_luma");
		int[] l = lum[lumCur];
		meLuma.use().i("uMode", 0).tex("uSrc", 0, color, 0).f("uLuma", LUMA[0], LUMA[1], LUMA[2]);
		CsrGl.draw(lumFbo[lumCur][0], lw[0], lh[0], vao);
		meLuma.i("uMode", 1);
		for (int i = 1; i < CsrMotion.LEVELS; i++) {
			meLuma.tex("uSrc", 0, l[i - 1], 0).f("uDstSize", lw[i], lh[i]);
			CsrGl.draw(lumFbo[lumCur][i], lw[i], lh[i], vao);
		}
		CsrTimers.end();
	}

	private void globalMotion(int prevL3, int currL3) {
		CsrTimers.begin("fg_gmv");
		gmGrid.use().tex("uPrev", 0, prevL3, 0).tex("uCurr", 1, currL3, 0);
		CsrGl.draw(gmGridFbo, 64, 36, vao);
		gmCost.use().tex("uGrid", 0, gmGridTex, 0);
		CsrGl.draw(gmCostFbo, 25, 13, vao);
		gmPick.use().tex("uCost", 0, gmCostTex, 0);
		CsrGl.draw(gmFbo, 1, 1, vao);
		CsrTimers.end();
	}

	/** FgV2 SearchPyramid: vectors from each pixel of {@code b} to its match in {@code a}. */
	private void search(int[] a, int[] b, int hist, int dstFbo, String dir) {
		CsrTimers.begin("fg_p8_" + dir);
		meBlock.use().tex("uA", 0, a[3], 0).tex("uB", 1, b[3], 0).i("uLSize", lw[3], lh[3]).i("uTiles", tw, th);
		CsrGl.draw(blockFbo, bw, bh, vao);
		meP8.use().tex("uA", 0, a[3], 0).tex("uB", 1, b[3], 0).i("uLSize", lw[3], lh[3])
				.tex("uBlock", 2, blockTex, 0).tex("uHist", 3, hist, 0).tex("uGmv", 4, gmTex, 0)
				.i("uHistValid", mvHistValid ? 1 : 0).f("uInSize", w, h);
		CsrGl.draw(tv8Fbo, tw, th, vao);
		CsrTimers.end();
		CsrTimers.begin("fg_refine_" + dir);
		meProp.use().tex("uA", 0, a[3], 0).tex("uB", 1, b[3], 0).i("uLSize", lw[3], lh[3])
				.tex("uMv", 2, tv8, 0).i("uTiles", tw, th);
		CsrGl.draw(tv8sFbo, tw, th, vao);
		meP4.use().tex("uA", 0, a[2], 0).tex("uB", 1, b[2], 0).i("uLSize", lw[2], lh[2])
				.tex("uMv", 2, tv8s, 0).i("uTiles", tw, th);
		CsrGl.draw(tv4Fbo, tw, th, vao);
		meP2.use().tex("uA", 0, a[1], 0).tex("uB", 1, b[1], 0).i("uLSize", lw[1], lh[1])
				.tex("uMv", 2, tv4, 0).i("uTiles", tw, th).f("uInSize", w, h);
		CsrGl.draw(dstFbo, tw, th, vao);
		CsrTimers.end();
	}

	private void runFlowAndNets(int prev, int curr) {
		CsrTimers.begin("fg_pyr");
		// Luma pyramid: 1/4 (box 4x4 from colour), 1/8, 1/16 + mip chain for the frame mean.
		pyr.use().i("uMode", 0).tex("uPrev", 0, prev, 0).tex("uCurr", 1, curr, 0).tex("uSrc", 2, prev, 0)
				.i("uSrcSize", w, h).f("uLuma", LUMA[0], LUMA[1], LUMA[2]);
		CsrGl.draw(pyr4Fbo, qw, qh, vao);
		pyr.i("uMode", 1).tex("uSrc", 2, pyr4, 0).i("uSrcSize", qw, qh);
		CsrGl.draw(pyr8Fbo, ew, eh, vao);
		pyr.tex("uSrc", 2, pyr8, 0).i("uSrcSize", ew, eh);
		CsrGl.draw(pyr16Fbo, sw, sh, vao);
		CsrGl.generateMips(pyr16);

		CsrTimers.end();

		int[] prevL = lum[lumCur ^ 1];
		int[] currL = lum[lumCur];
		globalMotion(prevL[3], currL[3]);
		int hist = mvCur;
		mvCur ^= 1;
		search(prevL, currL, mv[0][hist], mvFbo[0][mvCur], "bwd");
		search(currL, prevL, mv[1][hist], mvFbo[1][mvCur], "fwd");
		mvHistValid = true;

		CsrTimers.begin("fg_occl");
		meOccl.use().tex("uBwd", 0, mv[0][mvCur], 0).tex("uFwd", 1, mv[1][mvCur], 0).f("uTiles", tw, th);
		CsrGl.draw(occlFbo, tw, th, vao);
		CsrTimers.end();

		CsrTimers.begin("fg_packa");
		packA.use().tex("uPyr4", 0, pyr4, 0).tex("uMv", 1, mv[0][mvCur], 0).tex("uOccl", 2, occlTex, 0)
				.tex("uCurrFinal", 3, curr, 0).tex("uCurrWorld", 4, worldTex, 0).tex("uPrev", 5, prev, 0)
				.i("uQSize", qw, qh).i("uTiles", tw, th).f("uFlowScale", weights.flowScale());
		common(packA);
		CsrGl.draw(packAFbo, qw, qh, vao);
		CsrTimers.end();

		CsrTimers.begin("fg_netA");
		int[] a = netA.run(conv, vao, packATex);
		CsrTimers.end();
		CsrTimers.begin("fg_packb");

		packB.use().tex("uPrev", 0, prev, 0).tex("uCurr", 1, curr, 0).i("uCurrFinal", 1)
				.tex("uPackA0", 2, packATex[0], 0).tex("uPackA1", 3, packATex[1], 0)
				.tex("uNetA0", 4, a[0], 0).tex("uNetA1", 5, a[1], 0).tex("uCurrWorld", 6, worldTex, 0)
				.f("uHSize", hw, hh).f("uT", T).f("uFlowScale", weights.flowScale());
		common(packB);
		CsrGl.draw(packBFbo, hw, hh, vao);
		CsrTimers.end();

		lastNetA = a;
		CsrTimers.begin("fg_netB");
		lastNetB = netB.run(conv, vao, packBTex);
		CsrTimers.end();
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
		CsrTimers.begin("fg_compose");
		CsrGl.draw(fbo, w, h, vao);
		CsrTimers.end();
	}

	private void common(CsrPass p) {
		p.f("uLuma", LUMA[0], LUMA[1], LUMA[2]).f("uFullSizeF", w, h).i("uFullSize", w, h);
	}

	private void releaseTargets() {
		CsrGl.deleteFbos(histFbo[0], histFbo[1], worldFbo, pyr4Fbo, pyr8Fbo, pyr16Fbo, packAFbo, packBFbo, mainFbo,
				gmGridFbo, gmCostFbo, gmFbo, blockFbo, tv8Fbo, tv8sFbo, tv4Fbo, occlFbo);
		CsrGl.deleteTextures(hist[0], hist[1], worldTex, pyr4, pyr8, pyr16,
				packATex[0], packATex[1], packBTex[0], packBTex[1], packBTex[2],
				gmGridTex, gmCostTex, gmTex, blockTex, tv8, tv8s, tv4, occlTex);
		for (int i = 0; i < 2; i++) {
			CsrGl.deleteFbos(lumFbo[i]);
			CsrGl.deleteTextures(lum[i]);
			CsrGl.deleteFbos(mvFbo[i]);
			CsrGl.deleteTextures(mv[i]);
			java.util.Arrays.fill(lum[i], 0);
			java.util.Arrays.fill(lumFbo[i], 0);
			java.util.Arrays.fill(mv[i], 0);
			java.util.Arrays.fill(mvFbo[i], 0);
		}
		mvHistValid = false;
		hist[0] = hist[1] = histFbo[0] = histFbo[1] = 0;
		mainFbo = mainFboTex = 0;
		havePrev = false;
	}

	@Override
	public void close() {
		CsrGl.Saved saved = CsrGl.Saved.capture();
		try {
			releaseTargets();
			CsrTimers.close();
			netA.close();
			netB.close();
			pyr.close();
			for (CsrPass p : new CsrPass[] {meLuma, gmGrid, gmCost, gmPick, meBlock, meP8, meProp, meP4, meP2, meOccl}) {
				p.close();
			}
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
