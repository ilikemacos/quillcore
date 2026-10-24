package com.chopstickshq.quillcore.csr;

import java.util.List;

/** CSR Flux 2x upscaler weights (format version 1): luma in, 4 shuffle channels out. */
public record CsrFluxWeights(float[] luma, List<CsrLayer> layers) {
	public static final int VERSION = 1;
	public static final int SCALE = 2;

	public CsrFluxWeights {
		if (luma == null || luma.length != 3) {
			throw new IllegalArgumentException("luma must have 3 coefficients");
		}
		if (layers == null || layers.isEmpty()) {
			throw new IllegalArgumentException("no layers");
		}
		layers = List.copyOf(layers);
		if (layers.get(0).in() != 1) {
			throw new IllegalArgumentException("first layer must take 1 (luma) channel");
		}
		if (layers.get(layers.size() - 1).out() != SCALE * SCALE) {
			throw new IllegalArgumentException("last layer must output " + (SCALE * SCALE) + " shuffle channels");
		}
		checkChain(layers);
	}

	static void checkChain(List<CsrLayer> layers) {
		for (int i = 1; i < layers.size(); i++) {
			if (layers.get(i).in() != layers.get(i - 1).out()) {
				throw new IllegalArgumentException("layer " + i + " input " + layers.get(i).in()
						+ " does not match previous output " + layers.get(i - 1).out());
			}
		}
	}

	public CsrFluxWeights zeroed() {
		return new CsrFluxWeights(luma, layers.stream().map(CsrLayer::zeroed).toList());
	}
}
