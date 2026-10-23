package com.chopstickshq.quillcore.render;

/**
 * Caps vanilla cloud-range (in chunks) further when enabled.
 * Does not replace the vanilla Cloud Distance option — only a lower bound on top of it.
 */
public final class CloudDistanceCap {
	private CloudDistanceCap() {}

	/** @param vanillaChunks result of {@code min(options.cloudRange, 128)} */
	public static int applyChunkCap(
			boolean masterEnabled,
			boolean renderEnabled,
			boolean capEnabled,
			int capChunks,
			int vanillaChunks
	) {
		if (!masterEnabled || !renderEnabled || !capEnabled) {
			return vanillaChunks;
		}
		int cap = Math.max(1, capChunks);
		return Math.min(vanillaChunks, cap);
	}
}
