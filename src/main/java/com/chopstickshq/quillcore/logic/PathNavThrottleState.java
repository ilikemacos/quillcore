package com.chopstickshq.quillcore.logic;

import java.util.HashMap;
import java.util.Map;

/** Per-mob consecutive PathNavigation skip counters. */
public final class PathNavThrottleState {
	private static final Map<Integer, Integer> CONSECUTIVE = new HashMap<>();

	private PathNavThrottleState() {}

	public static int consecutiveSkips(int entityId) {
		return CONSECUTIVE.getOrDefault(entityId, 0);
	}

	public static void record(int entityId, boolean skipped) {
		int next = IdleAiThrottle.nextConsecutiveSkips(skipped, consecutiveSkips(entityId));
		if (next == 0) {
			CONSECUTIVE.remove(entityId);
		} else {
			CONSECUTIVE.put(entityId, next);
		}
	}

	public static void clear() {
		CONSECUTIVE.clear();
	}
}
