package com.chopstickshq.quillcore.bench;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BenchReportTest {
	private static BenchMetrics m(Double fps, Double mspt) {
		BenchMetrics b = new BenchMetrics();
		b.avgFps = fps;
		b.tickDurationMs = mspt;
		return b;
	}

	@Test
	void meanIgnoresMissingAndOtherLabels() {
		BenchReport r = new BenchReport();
		r.add("on", m(100.0, 5.0));
		r.add("off", m(80.0, 6.0));
		r.add("on", m(120.0, null));
		assertEquals(110.0, r.mean("on", x -> x.avgFps), 1e-9);
		assertEquals(5.0, r.mean("on", x -> x.tickDurationMs), 1e-9);
		assertNull(r.mean("missing", x -> x.avgFps));
	}

	@Test
	void percentDeltaHandlesNullAndZero() {
		assertEquals(25.0, BenchReport.percentDelta(80.0, 100.0), 1e-9);
		assertNull(BenchReport.percentDelta(null, 1.0));
		assertNull(BenchReport.percentDelta(0.0, 1.0));
	}

	@Test
	void csvLeavesUnmeasuredCellsEmpty() {
		BenchReport r = new BenchReport();
		r.add("off", m(null, 4.5));
		String[] lines = r.toCsv().split("\n");
		assertEquals(BenchReport.CSV_HEADER, lines[0]);
		assertEquals("0,off,,,,,4.500,", lines[1]);
	}

	@Test
	void jsonWritesNullForMissing() {
		BenchReport r = new BenchReport().environment("test \"env\"");
		r.add("on", m(60.0, null));
		String json = r.toJson();
		assertTrue(json.contains("\"avgFps\": {\"on\": 60.000, \"off\": null, \"onVsOffPct\": null}"), json);
		assertTrue(json.contains("\"msptAvg\": {\"on\": null"), json);
		assertTrue(json.contains("test \\\"env\\\""));
	}

	@Test
	void writesBothFiles() throws Exception {
		Path dir = Files.createTempDirectory("qbench");
		BenchReport r = new BenchReport();
		r.add("on", m(1.0, 1.0));
		r.write(dir);
		assertTrue(Files.size(dir.resolve("bench.json")) > 0);
		assertTrue(Files.readString(dir.resolve("bench.csv")).startsWith("segment,"));
	}

	@Test
	void harnessFeedsReport() {
		BenchHarness.start("on");
		for (int i = 0; i < 100; i++) BenchHarness.recordFrameNanos(10_000_000L);
		BenchHarness.recordTickNanos(2_000_000L);
		BenchMetrics s = BenchHarness.snapshot();
		BenchHarness.stop();
		assertEquals(100.0, s.avgFps, 1e-6);
		assertEquals(2.0, s.tickDurationMs, 1e-6);
		assertEquals(10.0, s.frameTimeP99Ms, 1e-6);
	}

	@Test
	void fpsPerSecondFromFrameTimes() {
		java.util.List<Long> f = new java.util.ArrayList<>();
		for (int i = 0; i < 250; i++) f.add(10_000_000L); // 2.5 s at 100 fps
		assertEquals(java.util.List.of(100.0, 100.0), BenchReport.fpsPerSecond(f));
	}

	@Test
	void downsampleKeepsSpikes() {
		java.util.List<Long> f = new java.util.ArrayList<>(java.util.Collections.nCopies(100, 5_000_000L));
		f.set(42, 50_000_000L);
		java.util.List<Double> d = BenchReport.downsampleMs(f, 10);
		assertEquals(10, d.size());
		assertEquals(50.0, d.get(4), 1e-9);
	}

	@Test
	void seriesInJson() {
		BenchReport r = new BenchReport();
		r.add("on", m(100.0, 1.0), java.util.Collections.nCopies(100, 10_000_000L));
		assertTrue(r.toJson().contains("\"fpsPerSecond\": [100.000]"), r.toJson());
	}
}
