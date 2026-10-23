package com.chopstickshq.quillcore.logic;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-tick skip flag (ThreadLocal) and per-entity consecutive skip counters.
 * Counters are best-effort; entity id reuse is acceptable for a throttle heuristic.
 */
public final class IdleAiThrottleState {
	private static final ThreadLocal<Boolean> SKIP_THIS_TICK = ThreadLocal.withInitial(() -> Boolean.FALSE);
	private static final Map<Integer, Integer> CONSECUTIVE = new HashMap<>();

	private IdleAiThrottleState() {}

	public static void beginTick(boolean skip) {
		SKIP_THIS_TICK.set(skip);
	}

	public static boolean shouldSkipSelectors() {
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

	/** Test helper / config reload. */
	public static void clear() {
		SKIP_THIS_TICK.set(Boolean.FALSE);
		CONSECUTIVE.clear();
	}
}
