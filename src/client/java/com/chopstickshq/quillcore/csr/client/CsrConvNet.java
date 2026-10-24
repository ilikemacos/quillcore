package com.chopstickshq.quillcore.csr.client;

import com.chopstickshq.quillcore.csr.CsrCost;
import com.chopstickshq.quillcore.csr.CsrLayer;

import java.util.List;

/**
 * A stack of conv3x3 layers on the GPU. Each layer writes ceil(out/4) RGBA16F slabs through MRT;
 * the next layer reads them with texelFetch. Owns its weight texture and per-layer targets.
 */
final class CsrConvNet implements AutoCloseable {
	private static final int WEIGHT_UNIT = CsrGl.MAX_SLABS;

	private final List<CsrLayer> layers;
	private final int weightTex;
	private final int[] wOff;
	private final int[] bOff;
	private int width;
	private int height;
	private int[][] outTex;
	private int[] fbo;

	CsrConvNet(List<CsrLayer> layers) {
		for (CsrLayer l : layers) {
			if (CsrCost.slabs(l.in()) > CsrGl.MAX_SLABS || CsrCost.slabs(l.out()) > CsrGl.MAX_SLABS) {
				throw new CsrGl.GlException("layer " + l.in() + "->" + l.out() + " exceeds " + (4 * CsrGl.MAX_SLABS) + " channels");
			}
		}
		this.layers = layers;
		this.wOff = new int[layers.size()];
		this.bOff = new int[layers.size()];
		this.weightTex = CsrGl.weightTexture(layers, wOff, bOff);
	}

	void ensure(int w, int h) {
		if (outTex != null && w == width && h == height) {
			return;
		}
		releaseTargets();
		width = w;
		height = h;
		outTex = new int[layers.size()][];
		fbo = new int[layers.size()];
		for (int i = 0; i < layers.size(); i++) {
			int n = CsrCost.slabs(layers.get(i).out());
			outTex[i] = new int[n];
			for (int s = 0; s < n; s++) {
				outTex[i][s] = CsrGl.featureTexture(w, h);
			}
			fbo[i] = CsrGl.fbo(outTex[i]);
		}
	}

	/** Runs every layer at the current size; returns the last layer's slabs. */
	int[] run(CsrPass conv, int vao, int[] input) {
		int[] src = input;
		conv.use().i("uSize", width, height);
		conv.i("uW", WEIGHT_UNIT);
		CsrGl.bind(WEIGHT_UNIT, weightTex, 0);
		for (int i = 0; i < layers.size(); i++) {
			CsrLayer l = layers.get(i);
			if (src.length < CsrCost.slabs(l.in())) {
				throw new CsrGl.GlException("layer " + i + " has " + src.length + " input slabs, needs " + CsrCost.slabs(l.in()));
			}
			for (int s = 0; s < CsrGl.MAX_SLABS; s++) {
				conv.tex("uIn" + s, s, s < src.length ? src[s] : src[0], 0);
			}
			conv.i("uInC", l.in()).i("uOutC", l.out()).i("uWOff", wOff[i]).i("uBOff", bOff[i]).i("uLeaky", l.leaky() ? 1 : 0);
			CsrGl.draw(fbo[i], width, height, vao);
			src = outTex[i];
		}
		return src;
	}

	private void releaseTargets() {
		if (outTex != null) {
			for (int[] t : outTex) {
				CsrGl.deleteTextures(t);
			}
			CsrGl.deleteFbos(fbo);
		}
		outTex = null;
		fbo = null;
	}

	@Override
	public void close() {
		releaseTargets();
		CsrGl.deleteTextures(weightTex);
	}
}
