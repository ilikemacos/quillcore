package com.chopstickshq.quillcore.settings;

import com.chopstickshq.quillcore.settings.SupportPromptPolicy.State;
import org.junit.jupiter.api.Test;

import static com.chopstickshq.quillcore.settings.SupportPromptPolicy.*;
import static org.junit.jupiter.api.Assertions.*;

class SupportPromptPolicyTest {
	private static final long DAY = 24L * 60 * 60 * 1000;
	private static final long T0 = 1_800_000_000_000L;

	private static State opens(State s, int n) {
		for (int i = 0; i < n; i++) s = recordOpen(s);
		return s;
	}

	@Test
	void firstEligibilityAfterTenOpens() {
		State s = opens(new State(0, 0, false), 9);
		assertFalse(shouldShow(s, T0));
		s = recordOpen(s);
		assertTrue(shouldShow(s, T0));
	}

	@Test
	void needsBothTenOpensAndSevenDays() {
		State shown = recordShown(opens(new State(0, 0, false), 10), T0);
		assertEquals(0, shown.openCount());
		assertFalse(shouldShow(opens(shown, 50), T0 + 6 * DAY), "too soon");
		assertFalse(shouldShow(opens(shown, 9), T0 + 30 * DAY), "too few opens");
		assertTrue(shouldShow(opens(shown, 10), T0 + 7 * DAY));
	}

	@Test
	void neverShowIsPermanent() {
		State s = recordNeverShow(opens(new State(0, 0, false), 100));
		assertFalse(shouldShow(s, T0 + 1000 * DAY));
		assertFalse(shouldShow(opens(recordShown(s, T0), 20), T0 + 1000 * DAY));
	}

	@Test
	void clockGoingBackwardsStaysQuiet() {
		State s = opens(recordShown(new State(10, 0, false), T0), 10);
		assertFalse(shouldShow(s, T0 - DAY));
	}

	@Test
	void countersSanitizedAndSaturate() {
		State s = new State(-5, -1, false);
		assertEquals(0, s.openCount());
		assertEquals(0, s.lastShownEpochMs());
		assertEquals(Integer.MAX_VALUE, recordOpen(new State(Integer.MAX_VALUE, 0, false)).openCount());
	}
}
