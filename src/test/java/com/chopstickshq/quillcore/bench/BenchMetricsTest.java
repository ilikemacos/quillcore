package com.chopstickshq.quillcore.bench;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BenchMetricsTest {
	@Test
	void emptyMetricsReportNoSamples() {
		BenchMetrics m = new BenchMetrics();
		m.scenarioId = "x";
		assertFalse(m.hasMeasurements());
		assertTrue(m.toString().contains("NO_SAMPLES"));
	}

	@Test
	void outOfRangeSamplesRejected() {
		BenchHarness.start("range");
		BenchHarness.recordFrameNanos(0L);
		BenchHarness.recordFrameNanos(-5L);
		BenchHarness.recordFrameNanos(6_000_000_000L);
		BenchMetrics m = BenchHarness.snapshot();
		BenchHarness.stop();
		assertNull(m.avgFps);
	}

	@Test
	void samplesIgnoredWhenStopped() {
		BenchHarness.start("s");
		BenchHarness.stop();
		BenchHarness.recordFrameNanos(16_000_000L);
		assertNull(BenchHarness.snapshot().avgFps);
	}

	@Test
	void percentilesOrdered() {
		BenchHarness.start("p");
		for (int i = 1; i <= 200; i++) BenchHarness.recordFrameNanos(i * 100_000L);
		BenchMetrics m = BenchHarness.snapshot();
		BenchHarness.stop();
		assertTrue(m.frameTimeP50Ms <= m.frameTimeP99Ms);
		assertTrue(m.pct1LowFps <= m.avgFps);
	}
}
