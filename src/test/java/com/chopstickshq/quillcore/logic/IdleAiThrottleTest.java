package com.chopstickshq.quillcore.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdleAiThrottleTest {
	private static final double RADIUS = 32.0;
	private static final double FAR = (RADIUS + 1) * (RADIUS + 1);
	private static final double NEAR = (RADIUS - 1) * (RADIUS - 1);

	@Test
	void disabledMasterNeverSkips() {
		assertFalse(IdleAiThrottle.shouldSkipGoalTick(false, true, true, 2, RADIUS, FAR, false, true, 0, 0));
	}

	@Test
	void disabledLogicNeverSkips() {
		assertFalse(IdleAiThrottle.shouldSkipGoalTick(true, false, true, 2, RADIUS, FAR, false, true, 0, 0));
	}

	@Test
	void disabledThrottleNeverSkips() {
		assertFalse(IdleAiThrottle.shouldSkipGoalTick(true, true, false, 2, RADIUS, FAR, false, true, 0, 0));
	}

	@Test
	void attackTargetNeverSkips() {
		assertFalse(IdleAiThrottle.shouldSkipGoalTick(true, true, true, 2, RADIUS, FAR, true, true, 0, 0));
	}

	@Test
	void hurtNeverSkips() {
		assertFalse(IdleAiThrottle.shouldSkipGoalTick(true, true, true, 2, RADIUS, FAR, false, true, 5, 0));
	}

	@Test
	void nearPlayerNeverSkips() {
		assertFalse(IdleAiThrottle.shouldSkipGoalTick(true, true, true, 2, RADIUS, NEAR, false, true, 0, 0));
	}

	@Test
	void deadNeverSkips() {
		assertFalse(IdleAiThrottle.shouldSkipGoalTick(true, true, true, 2, RADIUS, FAR, false, false, 0, 0));
	}

	@Test
	void zeroMaxNeverSkips() {
		assertFalse(IdleAiThrottle.shouldSkipGoalTick(true, true, true, 0, RADIUS, FAR, false, true, 0, 0));
	}

	@Test
	void farIdleSkipsUntilMaxThenRuns() {
		assertTrue(IdleAiThrottle.shouldSkipGoalTick(true, true, true, 2, RADIUS, FAR, false, true, 0, 0));
		assertTrue(IdleAiThrottle.shouldSkipGoalTick(true, true, true, 2, RADIUS, FAR, false, true, 0, 1));
		assertFalse(IdleAiThrottle.shouldSkipGoalTick(true, true, true, 2, RADIUS, FAR, false, true, 0, 2));
	}

	@Test
	void consecutiveCounterResetsOnRun() {
		assertEquals(1, IdleAiThrottle.nextConsecutiveSkips(true, 0));
		assertEquals(2, IdleAiThrottle.nextConsecutiveSkips(true, 1));
		assertEquals(0, IdleAiThrottle.nextConsecutiveSkips(false, 2));
	}

	@Test
	void clampBounds() {
		assertEquals(0, IdleAiThrottle.clamp(-3, 0, 10));
		assertEquals(10, IdleAiThrottle.clamp(99, 0, 10));
		assertEquals(4, IdleAiThrottle.clamp(4, 0, 10));
	}
}
