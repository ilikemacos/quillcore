package com.chopstickshq.quillcore.csr;

import java.util.List;

/**
 * CPU reference of the CSR Flux 2x network, matching the reference kernels
 * (CSRAI_Luma → CSRAI_Conv × N → CSRAI_Shuffle). The optional sharpen pass is not part
 * of the network and is not modelled here. Used by tests and as the spec for the GLSL port.
 */
public final class CsrFluxCpu {
	private CsrFluxCpu() {}

	/**
	 * @param rgb interleaved linear RGB in [0,1], {@code w * h * 3} floats, row-major, y down
	 * @return interleaved RGB of size {@code 2w * 2h * 3}
	 */
	public static float[] upscale(CsrFluxWeights weights, float[] rgb, int w, int h) {
		if (w <= 0 || h <= 0 || rgb == null || rgb.length != w * h * 3) {
			throw new IllegalArgumentException("bad image " + w + "x" + h);
		}
		float[] luma = weights.luma();
		float[] feat = new float[w * h];
		for (int i = 0; i < w * h; i++) {
			feat[i] = luma(rgb, i * 3, luma);
		}
		int ch = 1;
		for (CsrLayer layer : weights.layers()) {
			feat = conv(layer, feat, w, h);
			ch = layer.out();
		}
		if (ch != 4) {
			throw new IllegalStateException("network must end with 4 channels");
		}
		int ow = w * 2, oh = h * 2;
		float[] out = new float[ow * oh * 3];
		float[] tap = new float[3];
		for (int y = 0; y < oh; y++) {
			for (int x = 0; x < ow; x++) {
				int c = ((y & 1) << 1) | (x & 1);
				float yDelta = feat[(c * h + (y >> 1)) * w + (x >> 1)];
				bilinear(rgb, w, h, (x + 0.5f) * 0.5f, (y + 0.5f) * 0.5f, tap);
				float yUp = tap[0] * luma[0] + tap[1] * luma[1] + tap[2] * luma[2];
				float add = (yDelta + yUp) - yUp;
				int o = (y * ow + x) * 3;
				for (int k = 0; k < 3; k++) {
					out[o + k] = saturate(tap[k] + add);
				}
			}
		}
		return out;
	}

	/** Bilinear upscale with the same sample positions as the residual path. */
	public static float[] bilinearUpscale(float[] rgb, int w, int h) {
		int ow = w * 2, oh = h * 2;
		float[] out = new float[ow * oh * 3];
		float[] tap = new float[3];
		for (int y = 0; y < oh; y++) {
			for (int x = 0; x < ow; x++) {
				bilinear(rgb, w, h, (x + 0.5f) * 0.5f, (y + 0.5f) * 0.5f, tap);
				System.arraycopy(tap, 0, out, (y * ow + x) * 3, 3);
			}
		}
		return out;
	}

	/** Planar conv3x3: input {@code in * h * w}, output {@code out * h * w}. */
	static float[] conv(CsrLayer l, float[] src, int w, int h) {
		float[] dst = new float[l.out() * w * h];
		float[] wt = l.w();
		for (int oc = 0; oc < l.out(); oc++) {
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					float acc = l.b()[oc];
					for (int ic = 0; ic < l.in(); ic++) {
						int k = (oc * l.in() + ic) * 9;
						int plane = ic * h * w;
						for (int ky = 0; ky < 3; ky++) {
							int sy = clamp(y + ky - 1, h);
							for (int kx = 0; kx < 3; kx++) {
								int sx = clamp(x + kx - 1, w);
								acc += wt[k + ky * 3 + kx] * src[plane + sy * w + sx];
							}
						}
					}
					dst[(oc * h + y) * w + x] = l.activate(acc);
				}
			}
		}
		return dst;
	}

	/** Clamp-to-edge linear sample at texel-space position (px, py) where texel centres sit at +0.5. */
	static void bilinear(float[] rgb, int w, int h, float px, float py, float[] out) {
		float tx = px - 0.5f, ty = py - 0.5f;
		int x0 = (int) Math.floor(tx), y0 = (int) Math.floor(ty);
		float fx = tx - x0, fy = ty - y0;
		int xa = clamp(x0, w), xb = clamp(x0 + 1, w), ya = clamp(y0, h), yb = clamp(y0 + 1, h);
		for (int k = 0; k < 3; k++) {
			float v00 = rgb[(ya * w + xa) * 3 + k], v10 = rgb[(ya * w + xb) * 3 + k];
			float v01 = rgb[(yb * w + xa) * 3 + k], v11 = rgb[(yb * w + xb) * 3 + k];
			float top = v00 + (v10 - v00) * fx, bot = v01 + (v11 - v01) * fx;
			out[k] = top + (bot - top) * fy;
		}
	}

	private static float luma(float[] rgb, int o, float[] c) {
		return rgb[o] * c[0] + rgb[o + 1] * c[1] + rgb[o + 2] * c[2];
	}

	private static int clamp(int v, int n) {
		return v < 0 ? 0 : (v >= n ? n - 1 : v);
	}

	private static float saturate(float v) {
		return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
	}

	/** Total MACs per LR pixel for a layer stack. */
	public static long macsPerPixel(List<CsrLayer> layers) {
		long n = 0;
		for (CsrLayer l : layers) {
			n += l.macsPerPixel();
		}
		return n;
	}
}
