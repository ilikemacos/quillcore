package com.chopstickshq.quillcore.logic;

/**
 * Shared state for the villager Brain throttle. The per-tick skip flag and the consecutive-skip counter live on each entity (see the
 * mixin); this only holds an epoch so a config reload can reset every counter at once.
 */
public final class BrainIdleThrottleState {
	private static int epoch;

	private BrainIdleThrottleState() {}

	public static int epoch() {
		return epoch;
	}

	/** Test helper / config reload: forget every entity's consecutive skips. */
	public static void clear() {
		epoch++;
	}
}
