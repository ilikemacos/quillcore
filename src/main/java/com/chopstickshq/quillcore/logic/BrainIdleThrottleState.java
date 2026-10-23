package com.chopstickshq.quillcore.logic;

import java.util.HashMap;
import java.util.Map;

/** Per-villager consecutive Brain.tick skip counters + ThreadLocal skip flag. */
public final class BrainIdleThrottleState {
	private static final ThreadLocal<Boolean> SKIP_THIS_TICK = ThreadLocal.withInitial(() -> Boolean.FALSE);
	private static final Map<Integer, Integer> CONSECUTIVE = new HashMap<>();

	private BrainIdleThrottleState() {}

	public static void beginTick(boolean skip) {
		SKIP_THIS_TICK.set(skip);
	}

	public static boolean shouldSkipBrain() {
		return Boolean.TRUE.equals(SKIP_THIS_TICK.get());
	}

	public static void endTick(int entityId, boolean skipped) {
		SKIP_THIS_TICK.set(Boolean.FALSE);
		int next = IdleAiThrottle.nextConsecutiveSkips(skipped, consecutiveSkips(entityId));
		if (next == 0) {
			CONSECUTIVE.remove(entityId);
		} else {
			CONSECUTIVE.put(entityId, next);
		}
	}

	public static int consecutiveSkips(int entityId) {
		return CONSECUTIVE.getOrDefault(entityId, 0);
	}

	public static void clear() {
		SKIP_THIS_TICK.set(Boolean.FALSE);
		CONSECUTIVE.clear();
	}
}
