package com.chopstickshq.quillcore.logic;

/**
 * Pure decision logic for throttling {@code PathNavigation#tick} when a mob is idle,
 * far from players, and has no active path. Never interrupts follow-the-path.
 */
public final class PathNavThrottle {
	private PathNavThrottle() {}

	public static boolean shouldSkipNavTick(
			boolean masterEnabled,
			boolean logicEnabled,
			boolean throttleEnabled,
			int maxSkipTicks,
			double playerRadiusBlocks,
			double nearestPlayerDistSq,
			boolean pathDone,
			boolean hasAttackTarget,
			boolean isAlive,
			int hurtTime,
			int consecutiveSkips
	) {
		if (!masterEnabled || !logicEnabled || !throttleEnabled) {
			return false;
		}
		if (!isAlive || !pathDone || hasAttackTarget || hurtTime > 0) {
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
