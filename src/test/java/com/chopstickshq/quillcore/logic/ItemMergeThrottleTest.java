package com.chopstickshq.quillcore.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemMergeThrottleTest {
	@Test
	void disabledGatesNeverSkip() {
		assertFalse(ItemMergeThrottle.shouldSkipMergeScan(false, true, true, 2, 32, 999999, true, 0));
		assertFalse(ItemMergeThrottle.shouldSkipMergeScan(true, false, true, 2, 32, 999999, true, 0));
		assertFalse(ItemMergeThrottle.shouldSkipMergeScan(true, true, false, 2, 32, 999999, true, 0));
	}

	@Test
	void nearPlayerNeverSkips() {
		assertFalse(ItemMergeThrottle.shouldSkipMergeScan(true, true, true, 2, 32, 16 * 16, true, 0));
		assertFalse(ItemMergeThrottle.shouldSkipMergeScan(true, true, true, 2, 32, 32 * 32, true, 0));
	}

	@Test
	void farPlayerSkipsUntilCap() {
		assertTrue(ItemMergeThrottle.shouldSkipMergeScan(true, true, true, 2, 32, 64 * 64, true, 0));
		assertTrue(ItemMergeThrottle.shouldSkipMergeScan(true, true, true, 2, 32, 64 * 64, true, 1));
		assertFalse(ItemMergeThrottle.shouldSkipMergeScan(true, true, true, 2, 32, 64 * 64, true, 2));
	}

	@Test
	void deadEntityNeverSkips() {
		assertFalse(ItemMergeThrottle.shouldSkipMergeScan(true, true, true, 2, 32, 999999, false, 0));
	}

	@Test
	void zeroMaxSkipNeverSkips() {
		assertFalse(ItemMergeThrottle.shouldSkipMergeScan(true, true, true, 0, 32, 999999, true, 0));
	}
}
