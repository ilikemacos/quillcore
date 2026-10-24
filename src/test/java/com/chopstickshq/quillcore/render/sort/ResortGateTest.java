package com.chopstickshq.quillcore.render.sort;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResortGateTest {
	@Test
	void rebuiltSectionStillSortsWithoutSectionChange() {
		// A rebuild stores the compile-time point of view on the mesh. If the camera has a different one now,
		// newPov is true and the sort must happen even though the camera never left its section.
		ResortGate.beginFrame(8, 70, 8);
		ResortGate.beginFrame(9, 70, 8);
		assertTrue(ResortGate.shouldResort(false, false, false, true));
		assertTrue(ResortGate.shouldResort(true, false, true, true));
	}

	@Test
	void farAlignedWaitsForSectionChange() {
		ResortGate.beginFrame(8, 70, 8);
		ResortGate.beginFrame(9, 70, 8);
		assertFalse(ResortGate.shouldResort(true, false, true, false));
		ResortGate.beginFrame(17, 70, 8);
		assertTrue(ResortGate.shouldResort(true, false, true, false));
	}

	@Test
	void nearbyFollowsVanilla() {
		ResortGate.beginFrame(8, 70, 8);
		ResortGate.beginFrame(9, 70, 8);
		assertTrue(ResortGate.shouldResort(true, true, false, false));
		assertFalse(ResortGate.shouldResort(false, true, true, false));
	}
}
