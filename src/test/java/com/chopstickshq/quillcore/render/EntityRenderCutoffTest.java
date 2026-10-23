package com.chopstickshq.quillcore.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityRenderCutoffTest {
	private static final double CUTOFF = 128.0;
	private static final double FAR = (CUTOFF + 1) * (CUTOFF + 1);
	private static final double NEAR = (CUTOFF - 1) * (CUTOFF - 1);

	@Test
	void disabledGatesNeverSkip() {
		assertFalse(EntityRenderCutoff.shouldSkipRender(false, true, true, CUTOFF, FAR, false, false));
		assertFalse(EntityRenderCutoff.shouldSkipRender(true, false, true, CUTOFF, FAR, false, false));
		assertFalse(EntityRenderCutoff.shouldSkipRender(true, true, false, CUTOFF, FAR, false, false));
	}

	@Test
	void cameraAndVehicleNeverSkip() {
		assertFalse(EntityRenderCutoff.shouldSkipRender(true, true, true, CUTOFF, FAR, true, false));
		assertFalse(EntityRenderCutoff.shouldSkipRender(true, true, true, CUTOFF, FAR, false, true));
	}

	@Test
	void nearNeverSkips() {
		assertFalse(EntityRenderCutoff.shouldSkipRender(true, true, true, CUTOFF, NEAR, false, false));
	}

	@Test
	void farSkips() {
		assertTrue(EntityRenderCutoff.shouldSkipRender(true, true, true, CUTOFF, FAR, false, false));
	}

	@Test
	void zeroCutoffNeverSkips() {
		assertFalse(EntityRenderCutoff.shouldSkipRender(true, true, true, 0, FAR, false, false));
	}
}
