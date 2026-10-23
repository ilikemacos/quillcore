package com.chopstickshq.quillcore.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockEntityRenderCutoffTest {
	private static final double CUTOFF = 96.0;
	private static final double FAR = (CUTOFF + 1) * (CUTOFF + 1);

	@Test
	void onlyAllowlistedTypes() {
		assertTrue(BlockEntityRenderCutoff.isAllowlistedType("sign"));
		assertTrue(BlockEntityRenderCutoff.isAllowlistedType("hanging_sign"));
		assertTrue(BlockEntityRenderCutoff.isAllowlistedType("skull"));
		assertFalse(BlockEntityRenderCutoff.isAllowlistedType("chest"));
		assertFalse(BlockEntityRenderCutoff.isAllowlistedType("furnace"));
		assertFalse(BlockEntityRenderCutoff.isAllowlistedType(null));
	}

	@Test
	void chestNeverSkippedEvenWhenFar() {
		assertFalse(BlockEntityRenderCutoff.shouldSkipRender(true, true, true, CUTOFF, FAR, "chest"));
	}

	@Test
	void farSignSkipped() {
		assertTrue(BlockEntityRenderCutoff.shouldSkipRender(true, true, true, CUTOFF, FAR, "sign"));
	}

	@Test
	void disabledNeverSkips() {
		assertFalse(BlockEntityRenderCutoff.shouldSkipRender(false, true, true, CUTOFF, FAR, "sign"));
	}
}
