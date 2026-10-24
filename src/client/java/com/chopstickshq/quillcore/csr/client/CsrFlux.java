package com.chopstickshq.quillcore.csr.client;

import com.chopstickshq.quillcore.csr.CsrFluxWeights;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.opengl.GL33;

/**
 * CSR Flux on the GPU: luma → conv × 4 → pixel-shuffle + bilinear residual → sharpen,
 * from the half-resolution main target into a 2x target, then blitted into the restored main target.
 */
final class CsrFlux implements AutoCloseable {
	/** Reference CSRAI_Sharpen strength offset (exp2(-2) lobe scale). Fixed; not part of the network. */
	static final float SHARPNESS = 2.0f;

	private final CsrFluxWeights weights;
	private final CsrPass luma;
	private final CsrPass conv;
	private final CsrPass shuffle;
	private final CsrPass sharpen;
	private final CsrConvNet net;
	private final int vao;
	private final int linear;
	private int lrW;
	private int lrH;
	private int lumaTex;
	private int lumaFbo;
	private int shuffleTex;
	private int shuffleFbo;
	private int sharpTex;
	private int sharpFbo;

	CsrFlux(CsrFluxWeights weights) {
		this.weights = weights;
		CsrGl.Saved saved = CsrGl.Saved.capture();
		try {
			luma = new CsrPass("flux_luma.fsh");
			conv = new CsrPass("conv.fsh");
			shuffle = new CsrPass("flux_shuffle.fsh");
			sharpen = new CsrPass("flux_sharpen.fsh");
			net = new CsrConvNet(weights.layers());
			vao = GlStateManager._glGenVertexArrays();
			linear = CsrGl.linearClampSampler();
			CsrGl.check("CSR Flux init");
		} finally {
			saved.restore();
		}
	}

	private void ensure(int w, int h) {
		if (lumaTex != 0 && w == lrW && h == lrH) {
			return;
		}
		releaseTargets();
		lrW = w;
		lrH = h;
		lumaTex = CsrGl.featureTexture(w, h);
		lumaFbo = CsrGl.fbo(lumaTex);
		net.ensure(w, h);
		shuffleTex = CsrGl.colorTexture(2 * w, 2 * h);
		shuffleFbo = CsrGl.fbo(shuffleTex);
		sharpTex = CsrGl.colorTexture(2 * w, 2 * h);
		sharpFbo = CsrGl.fbo(sharpTex);
	}

	/** Runs the network on the low-res main colour. Main must still be {@code w × h}. */
	void runNetwork(RenderTarget main, int w, int h) {
		CsrGl.Saved saved = CsrGl.Saved.capture();
		try {
			CsrGl.drainErrors();
			CsrGl.neutralState();
			ensure(w, h);
			int mainTex = ((GlTexture) main.getColorTexture()).glId();
			float[] lc = weights.luma();

			CsrTimers.begin("flux_luma");
			luma.use().tex("uColor", 0, mainTex, linear).f("uLuma", lc[0], lc[1], lc[2]);
			CsrGl.draw(lumaFbo, w, h, vao);
			CsrTimers.end();

			CsrTimers.begin("flux_net");
			int[] feat = net.run(conv, vao, new int[] {lumaTex});
			CsrTimers.end();

			shuffle.use().tex("uColor", 0, mainTex, linear).tex("uFeat", 1, feat[0], 0)
					.f("uLrSize", w, h).i("uLrSizeI", w, h).f("uLuma", lc[0], lc[1], lc[2]);
			CsrTimers.begin("flux_shuffle");
			CsrGl.draw(shuffleFbo, 2 * w, 2 * h, vao);
			CsrTimers.end();

			sharpen.use().tex("uColor", 0, shuffleTex, 0).i("uSize", 2 * w, 2 * h).f("uSharpness", SHARPNESS);
			CsrTimers.begin("flux_sharpen");
			CsrGl.draw(sharpFbo, 2 * w, 2 * h, vao);
			CsrTimers.end();
			CsrGl.check("CSR Flux passes");
		} finally {
			saved.restore();
		}
	}

	/** Copies the 2x result into the (already restored to native) main target. */
	void blitInto(RenderTarget main) {
		CsrGl.Saved saved = CsrGl.Saved.capture();
		int dst = 0;
		try {
			CsrGl.drainErrors();
			CsrGl.neutralState();
			dst = CsrGl.fbo(((GlTexture) main.getColorTexture()).glId());
			CsrGl.blit(sharpFbo, 2 * lrW, 2 * lrH, dst, main.width, main.height, true);
			CsrGl.check("CSR Flux blit");
		} finally {
			saved.restore();
			CsrGl.deleteFbos(dst);
		}
	}

	private void releaseTargets() {
		CsrGl.deleteFbos(lumaFbo, shuffleFbo, sharpFbo);
		CsrGl.deleteTextures(lumaTex, shuffleTex, sharpTex);
		lumaTex = lumaFbo = shuffleTex = shuffleFbo = sharpTex = sharpFbo = 0;
	}

	@Override
	public void close() {
		CsrGl.Saved saved = CsrGl.Saved.capture();
		try {
			releaseTargets();
			CsrTimers.close();
			net.close();
			luma.close();
			conv.close();
			shuffle.close();
			sharpen.close();
			GL33.glDeleteSamplers(linear);
			org.lwjgl.opengl.GL30.glDeleteVertexArrays(vao);
		} finally {
			saved.restore();
		}
	}
}
