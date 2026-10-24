package com.chopstickshq.quillcore.csr;

/**
 * How CSR Frame Gen spaces the interpolated present before the real one.
 * Minecraft presents on its render thread, so the only options are: let VSync queue
 * both presents, or under an FPS cap sleep part of the cap's slack. Uncapped without VSync,
 * there is nothing to pace against.
 */
public final class CsrPacing {
	/** Minecraft's "Unlimited" framerate slider value. */
	public static final int UNLIMITED_FPS = 260;
	/** Never hold the real frame longer than this. */
	public static final long MAX_GAP_NS = 20_000_000L;

	public enum Mode {
		/** Driver paces both swaps at vblank; no sleep. */
		VSYNC,
		/** Sleep half the capped frame period after the interpolated present. */
		CAPPED,
		/** Back-to-back presents; no smoothness benefit. */
		NONE
	}

	private CsrPacing() {}

	public static Mode mode(boolean vsync, int fpsLimit) {
		if (vsync) {
			return Mode.VSYNC;
		}
		return fpsLimit > 0 && fpsLimit < UNLIMITED_FPS ? Mode.CAPPED : Mode.NONE;
	}

	/** Target delay between the interpolated present and the real present. */
	public static long gapNanos(boolean vsync, int fpsLimit) {
		if (mode(vsync, fpsLimit) != Mode.CAPPED) {
			return 0L;
		}
		return Math.min(1_000_000_000L / fpsLimit / 2, MAX_GAP_NS);
	}
}
