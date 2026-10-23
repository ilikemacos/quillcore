package com.chopstickshq.quillcore.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleRenderCutoffTest {
	@Test
	void disabledGatesNeverSkip() {
		assertFalse(ParticleRenderCutoff.shouldSkipSpawn(false, true, true, 64, 999999));
		assertFalse(ParticleRenderCutoff.shouldSkipSpawn(true, false, true, 64, 999999));
		assertFalse(ParticleRenderCutoff.shouldSkipSpawn(true, true, false, 64, 999999));
	}

	@Test
	void withinCutoffDoesNotSkip() {
		assertFalse(ParticleRenderCutoff.shouldSkipSpawn(true, true, true, 64, 32 * 32));
		assertFalse(ParticleRenderCutoff.shouldSkipSpawn(true, true, true, 64, 64 * 64));
	}

	@Test
	void beyondCutoffSkips() {
		assertTrue(ParticleRenderCutoff.shouldSkipSpawn(true, true, true, 64, 65 * 65));
	}

	@Test
	void zeroOrNegativeCutoffNeverSkips() {
		assertFalse(ParticleRenderCutoff.shouldSkipSpawn(true, true, true, 0, 999999));
		assertFalse(ParticleRenderCutoff.shouldSkipSpawn(true, true, true, -10, 999999));
	}
}
