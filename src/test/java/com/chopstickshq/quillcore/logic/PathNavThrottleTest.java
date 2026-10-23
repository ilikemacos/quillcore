package com.chopstickshq.quillcore.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathNavThrottleTest {
	@Test
	void disabledGatesNeverSkip() {
		assertFalse(PathNavThrottle.shouldSkipNavTick(false, true, true, 2, 32, 999999, true, false, true, 0, 0));
		assertFalse(PathNavThrottle.shouldSkipNavTick(true, false, true, 2, 32, 999999, true, false, true, 0, 0));
		assertFalse(PathNavThrottle.shouldSkipNavTick(true, true, false, 2, 32, 999999, true, false, true, 0, 0));
	}

	@Test
	void activePathNeverSkips() {
		assertFalse(PathNavThrottle.shouldSkipNavTick(true, true, true, 2, 32, 999999, false, false, true, 0, 0));
	}

	@Test
	void targetOrHurtNeverSkips() {
		assertFalse(PathNavThrottle.shouldSkipNavTick(true, true, true, 2, 32, 999999, true, true, true, 0, 0));
		assertFalse(PathNavThrottle.shouldSkipNavTick(true, true, true, 2, 32, 999999, true, false, true, 5, 0));
	}

	@Test
	void farIdleSkipsUntilCap() {
		assertTrue(PathNavThrottle.shouldSkipNavTick(true, true, true, 2, 32, 64 * 64, true, false, true, 0, 0));
		assertTrue(PathNavThrottle.shouldSkipNavTick(true, true, true, 2, 32, 64 * 64, true, false, true, 0, 1));
		assertFalse(PathNavThrottle.shouldSkipNavTick(true, true, true, 2, 32, 64 * 64, true, false, true, 0, 2));
	}

	@Test
	void nearPlayerNeverSkips() {
		assertFalse(PathNavThrottle.shouldSkipNavTick(true, true, true, 2, 32, 16 * 16, true, false, true, 0, 0));
	}
}
