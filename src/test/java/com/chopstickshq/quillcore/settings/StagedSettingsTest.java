package com.chopstickshq.quillcore.settings;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StagedSettingsTest {
	private boolean liveToggle = false;
	private int liveDistance = 12;

	private StagedSettings make() {
		StagedSettings s = new StagedSettings();
		s.bind("toggle", () -> liveToggle, v -> liveToggle = v);
		s.bind("distance", () -> liveDistance, v -> liveDistance = v);
		return s;
	}

	@Test
	void stagedValuesAreNotLiveUntilApply() {
		StagedSettings s = make();
		s.stage("toggle", true);
		s.stage("distance", 20);
		assertFalse(liveToggle);
		assertEquals(12, liveDistance);
		assertEquals(Boolean.TRUE, s.get("toggle"));
		assertEquals(20, (int) s.get("distance"));
		assertEquals(2, s.pendingCount());

		assertEquals(List.of("toggle", "distance"), s.apply());
		assertTrue(liveToggle);
		assertEquals(20, liveDistance);
		assertFalse(s.hasPending());
	}

	@Test
	void undoRevertsPending() {
		StagedSettings s = make();
		s.stage("distance", 32);
		s.undo();
		assertFalse(s.hasPending());
		assertEquals(12, (int) s.get("distance"));
		assertTrue(s.apply().isEmpty());
		assertEquals(12, liveDistance);
	}

	@Test
	void stagingLiveValueClearsPending() {
		StagedSettings s = make();
		s.stage("distance", 16);
		assertTrue(s.isPending("distance"));
		s.stage("distance", 12);
		assertFalse(s.isPending("distance"));
	}

	@Test
	void unknownAndDuplicateIdsRejected() {
		StagedSettings s = make();
		assertThrows(IllegalArgumentException.class, () -> s.get("nope"));
		assertThrows(IllegalArgumentException.class, () -> s.bind("toggle", () -> 1, v -> {}));
	}
}
