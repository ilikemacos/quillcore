package com.chopstickshq.quillcore.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrainIdleThrottleTest {
	private static final double RADIUS = 32.0;
	private static final double FAR = (RADIUS + 1) * (RADIUS + 1);
	private static final double NEAR = (RADIUS - 1) * (RADIUS - 1);

	@Test
	void tradingNeverSkips() {
		assertFalse(BrainIdleThrottle.shouldSkipBrainTick(true, true, true, 2, RADIUS, FAR, true, true, 0, false, 0));
	}

	@Test
	void panicNeverSkips() {
		assertFalse(BrainIdleThrottle.shouldSkipBrainTick(true, true, true, 2, RADIUS, FAR, false, true, 0, true, 0));
	}

	@Test
	void hurtNeverSkips() {
		assertFalse(BrainIdleThrottle.shouldSkipBrainTick(true, true, true, 2, RADIUS, FAR, false, true, 3, false, 0));
	}

	@Test
	void nearNeverSkips() {
		assertFalse(BrainIdleThrottle.shouldSkipBrainTick(true, true, true, 2, RADIUS, NEAR, false, true, 0, false, 0));
	}

	@Test
	void farIdleSkipsUntilMax() {
		assertTrue(BrainIdleThrottle.shouldSkipBrainTick(true, true, true, 2, RADIUS, FAR, false, true, 0, false, 0));
		assertTrue(BrainIdleThrottle.shouldSkipBrainTick(true, true, true, 2, RADIUS, FAR, false, true, 0, false, 1));
		assertFalse(BrainIdleThrottle.shouldSkipBrainTick(true, true, true, 2, RADIUS, FAR, false, true, 0, false, 2));
	}

	@Test
	void disabledNeverSkips() {
		assertFalse(BrainIdleThrottle.shouldSkipBrainTick(false, true, true, 2, RADIUS, FAR, false, true, 0, false, 0));
		assertFalse(BrainIdleThrottle.shouldSkipBrainTick(true, false, true, 2, RADIUS, FAR, false, true, 0, false, 0));
		assertFalse(BrainIdleThrottle.shouldSkipBrainTick(true, true, false, 2, RADIUS, FAR, false, true, 0, false, 0));
	}
}
