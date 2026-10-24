package com.chopstickshq.quillcore.csr;

/**
 * Sizes and CPU reference math for the Frame Gen motion search (FrameFX FgV2). The GL passes in
 * {@code me_*.fsh} follow the same rules; the tests pin them down.
 */
public final class CsrMotion {
	/** Luma pyramid levels: full res, 1/2, 1/4, 1/8. The tile search runs on the last one. */
	public static final int LEVELS = 4;
	public static final int TILE = 8;

	private CsrMotion() {}

	/** Next pyramid level: floor halving, never below one texel. */
	public static int halve(int n) {
		return Math.max(1, n / 2);
	}

	/** Motion tiles per axis: one vector per 8x8 full-res pixels, rounded up. */
	public static int tiles(int n) {
		return Math.max(1, (n + TILE - 1) / TILE);
	}

	/** Groups of 8x8 tiles that share one block-consensus vector. */
	public static int blocks(int tiles) {
		return (tiles + TILE - 1) / TILE;
	}

	/**
	 * FG2_Halve on the CPU: each destination texel is one bilinear, edge-clamped tap of the source
	 * at the destination texel centre. Row-major, {@code src.length == w * h}.
	 */
	public static float[] halve(float[] src, int w, int h) {
		int dw = halve(w), dh = halve(h);
		float[] out = new float[dw * dh];
		for (int y = 0; y < dh; y++) {
			float sy = (y + 0.5f) / dh * h - 0.5f;
			for (int x = 0; x < dw; x++) {
				float sx = (x + 0.5f) / dw * w - 0.5f;
				out[y * dw + x] = bilinear(src, w, h, sx, sy);
			}
		}
		return out;
	}

	private static float bilinear(float[] src, int w, int h, float x, float y) {
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
		float fx = x - x0, fy = y - y0;
		float a = at(src, w, h, x0, y0), b = at(src, w, h, x0 + 1, y0);
		float c = at(src, w, h, x0, y0 + 1), d = at(src, w, h, x0 + 1, y0 + 1);
		return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
	}

	private static float at(float[] src, int w, int h, int x, int y) {
		return src[Math.clamp(y, 0, h - 1) * w + Math.clamp(x, 0, w - 1)];
	}
}
