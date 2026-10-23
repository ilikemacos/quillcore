package com.chopstickshq.quillcore.render;

/**
 * Caps rain/snow column gather radius (vanilla fancy=10 / fast=5 blocks from camera XZ).
 * Off by default. Low values show a hard edge.
 */
public final class WeatherRadiusCap {
	private WeatherRadiusCap() {}

	public static int applyRadius(
			boolean masterEnabled,
			boolean renderEnabled,
			boolean capEnabled,
			int capBlocks,
			int vanillaRadius
	) {
		if (!masterEnabled || !renderEnabled || !capEnabled) {
			return vanillaRadius;
		}
		int cap = Math.max(1, capBlocks);
		return Math.min(vanillaRadius, cap);
	}
}
