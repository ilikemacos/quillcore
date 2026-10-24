package com.chopstickshq.quillcore.csr;

/**
 * One 3x3 convolution layer (stride 1, clamp-to-edge borders).
 * Weights use the reference layout {@code w[(oc * in + ic) * 9 + ky * 3 + kx]}.
 */
public record CsrLayer(int in, int out, boolean leaky, float[] w, float[] b) {
	public static final float LEAKY_SLOPE = 0.1f;

	public CsrLayer {
		if (in <= 0 || out <= 0) {
			throw new IllegalArgumentException("layer channels must be positive: " + in + "->" + out);
		}
		if (w == null || w.length != in * out * 9) {
			throw new IllegalArgumentException("layer " + in + "->" + out + " expects " + (in * out * 9)
					+ " weights, got " + (w == null ? 0 : w.length));
		}
		if (b == null || b.length != out) {
			throw new IllegalArgumentException("layer " + in + "->" + out + " expects " + out
					+ " biases, got " + (b == null ? 0 : b.length));
		}
		for (float v : w) {
			if (!Float.isFinite(v)) {
				throw new IllegalArgumentException("non-finite weight");
			}
		}
		for (float v : b) {
			if (!Float.isFinite(v)) {
				throw new IllegalArgumentException("non-finite bias");
			}
		}
	}

	/** Multiply-accumulates per output pixel. */
	public int macsPerPixel() {
		return in * out * 9;
	}

	public float activate(float x) {
		return leaky && x < 0.0f ? LEAKY_SLOPE * x : x;
	}

	/** Same shape, all weights and biases zero. */
	public CsrLayer zeroed() {
		return new CsrLayer(in, out, leaky, new float[w.length], new float[b.length]);
	}
}
