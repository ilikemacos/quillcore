package com.chopstickshq.quillcore.bench;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BenchHarnessTest {
	@Test
	void emptyReportDoesNotInventFps() {
		BenchHarness.stop();
		BenchMetrics m = BenchHarness.snapshot();
		assertNull(m.avgFps);
		assertTrue(m.notes.contains("no samples") || m.notes.contains("refuse") || m.notes.contains("NO_SAMPLES") || !m.hasMeasurements() || m.tickDurationMs == null);
	}

	@Test
	void recordsTickAndFrameSamples() {
		BenchHarness.start("unit");
		BenchHarness.recordTickNanos(10_000_000L); // 10ms
		BenchHarness.recordFrameNanos(16_000_000L);
		BenchHarness.recordFrameNanos(17_000_000L);
		BenchMetrics m = BenchHarness.snapshot();
		assertNotNull(m.tickDurationMs);
		assertTrue(m.tickDurationMs > 9.0 && m.tickDurationMs < 11.0);
		assertNotNull(m.avgFps);
		assertTrue(m.avgFps > 50.0 && m.avgFps < 70.0);
		BenchHarness.stop();
	}
}
