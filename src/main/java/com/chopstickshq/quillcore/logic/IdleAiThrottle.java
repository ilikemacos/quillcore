package com.chopstickshq.quillcore.logic;

/**
 * Skips goal selector ticks for mobs that are far from every player, have no
 * target and weren't hurt recently. After maxSkipTicks skips in a row the next
 * tick always runs.
 */
public final class IdleAiThrottle {
	private IdleAiThrottle() {}

	/** nearestPlayerDistSq is POSITIVE_INFINITY when there is no player. */
	public static boolean shouldSkipGoalTick(
			boolean masterEnabled,
			boolean logicEnabled,
			boolean throttleEnabled,
			int maxSkipTicks,
			double playerRadiusBlocks,
			double nearestPlayerDistSq,
			boolean hasAttackTarget,
			boolean isAlive,
			int hurtTime,
			int consecutiveSkips
	) {
		if (!masterEnabled || !logicEnabled || !throttleEnabled) {
			return false;
		}
		if (!isAlive) {
			return false;
		}
		if (hasAttackTarget) {
			return false;
		}
		if (hurtTime > 0) {
			return false;
		}
		int clampedMax = clamp(maxSkipTicks, 0, 10);
		if (clampedMax <= 0) {
			return false;
		}
		double radius = Math.max(0.0, playerRadiusBlocks);
		double radiusSq = radius * radius;
		if (nearestPlayerDistSq <= radiusSq) {
			return false;
		}
		return consecutiveSkips < clampedMax;
	}

	public static int nextConsecutiveSkips(boolean skipped, int consecutiveSkips) {
		return skipped ? consecutiveSkips + 1 : 0;
	}

	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
