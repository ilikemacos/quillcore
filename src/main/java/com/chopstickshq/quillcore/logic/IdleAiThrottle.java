package com.chopstickshq.quillcore.logic;

/**
 * Pure decision logic for idle distant-mob AI goal throttling.
 * No Minecraft imports — unit-testable on a headless JVM.
 *
 * Semantics: when enabled and a mob is far from players, has no attack target,
 * and is not recently hurt, skip GoalSelector ticks for up to {@code maxSkipTicks}
 * consecutive opportunities, then force one run. Never changes physics, sensing,
 * navigation, or customServerAiStep — those stay on the mixin side.
 */
public final class IdleAiThrottle {
	private IdleAiThrottle() {}

	/**
	 * @param consecutiveSkips how many goal ticks have already been skipped in a row for this mob
	 * @param nearestPlayerDistSq squared distance to nearest player, or {@link Double#POSITIVE_INFINITY} if none
	 * @param playerRadiusBlocks skip only when nearest player is farther than this (or absent)
	 * @return true if GoalSelector.tick / tickRunningGoals should be no-op this call
	 */
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
		// Allow up to clampedMax skips, then force a run (caller resets counter).
		return consecutiveSkips < clampedMax;
	}

	public static int nextConsecutiveSkips(boolean skipped, int consecutiveSkips) {
		return skipped ? consecutiveSkips + 1 : 0;
	}

	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
