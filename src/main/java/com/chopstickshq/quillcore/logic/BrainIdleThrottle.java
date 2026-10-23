package com.chopstickshq.quillcore.logic;

/**
 * Pure decision logic for idle distant villager Brain.tick throttling.
 * Same risk profile as {@link IdleAiThrottle}: bounded skips, never skip when
 * trading, hurt, near a player, or dead.
 */
public final class BrainIdleThrottle {
	private BrainIdleThrottle() {}

	public static boolean shouldSkipBrainTick(
			boolean masterEnabled,
			boolean logicEnabled,
			boolean throttleEnabled,
			int maxSkipTicks,
			double playerRadiusBlocks,
			double nearestPlayerDistSq,
			boolean isTrading,
			boolean isAlive,
			int hurtTime,
			boolean isPanicking,
			int consecutiveSkips
	) {
		if (!masterEnabled || !logicEnabled || !throttleEnabled) {
			return false;
		}
		if (!isAlive) {
			return false;
		}
		if (isTrading || isPanicking) {
			return false;
		}
		if (hurtTime > 0) {
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
