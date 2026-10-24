package com.chopstickshq.quillcore.csr;

import java.util.List;

/**
 * Static per-pixel cost of the GLSL passes, used by docs/csr.md and its tests.
 * Fetch counts are RGBA16F texel fetches: a conv layer reads ceil(in/4) attachments × 9 taps.
 */
public final class CsrCost {
	private CsrCost() {}

	public static long macs(List<CsrLayer> layers) {
		return CsrFluxCpu.macsPerPixel(layers);
	}

	public static int slabs(int channels) {
		return (channels + 3) / 4;
	}

	public static long convFetches(List<CsrLayer> layers) {
		long n = 0;
		for (CsrLayer l : layers) {
			n += slabs(l.in()) * 9L;
		}
		return n;
	}

	/** MACs for a full frame of {@code pixels} at the layer stack's resolution. */
	public static double gmac(List<CsrLayer> layers, long pixels) {
		return macs(layers) * (double) pixels / 1e9;
	}
}
