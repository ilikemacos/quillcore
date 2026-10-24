package com.chopstickshq.quillcore.csr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsrPacingTest {
	@Test
	void vsyncNeverSleeps() {
		assertEquals(CsrPacing.Mode.VSYNC, CsrPacing.mode(true, 60));
		assertEquals(0L, CsrPacing.gapNanos(true, 60));
	}

	@Test
	void capSleepsHalfThePeriodBounded() {
		assertEquals(CsrPacing.Mode.CAPPED, CsrPacing.mode(false, 60));
		assertEquals(1_000_000_000L / 60 / 2, CsrPacing.gapNanos(false, 60));
		assertEquals(CsrPacing.MAX_GAP_NS, CsrPacing.gapNanos(false, 10));
	}

	@Test
	void uncappedHasNoPacing() {
		assertEquals(CsrPacing.Mode.NONE, CsrPacing.mode(false, CsrPacing.UNLIMITED_FPS));
		assertEquals(0L, CsrPacing.gapNanos(false, CsrPacing.UNLIMITED_FPS));
	}
}
