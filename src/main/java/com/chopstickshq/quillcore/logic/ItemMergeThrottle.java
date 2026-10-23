package com.chopstickshq.quillcore.logic;

/**
 * Pure decision logic for throttling ItemEntity / ExperienceOrb neighbour-merge scans.
 * Only the merge scan is skipped. Pickup, physics and age are untouched.
 */
public final class ItemMergeThrottle {
	private ItemMergeThrottle() {}

	public static boolean shouldSkipMergeScan(
			boolean masterEnabled,
			boolean logicEnabled,
			boolean throttleEnabled,
			int maxSkipTicks,
			double playerRadiusBlocks,
			double nearestPlayerDistSq,
			boolean isAlive,
			int consecutiveSkips
	) {
		if (!masterEnabled || !logicEnabled || !throttleEnabled) {
			return false;
		}
		if (!isAlive) {
			return false;
		}
		int clampedMax = IdleAiThrottle.clamp(maxSkipTicks, 0, 10);
		if (clampedMax <= 0) {
			return false;
		}
		double radius = Math.max(0.0, playerRadiusBlocks);
		if (nearestPlayerDistSq <= radius * radius) {
			return false;
		}
		return consecutiveSkips < clampedMax;
	}
}
