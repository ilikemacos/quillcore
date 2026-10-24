package com.chopstickshq.quillcore.bench;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight in-memory sampler. Does not invent FPS or comparison numbers.
 * Sodium/Lithium baseline cells stay empty until measured on real hardware.
 */
public final class BenchHarness {
	private static boolean running;
	private static String scenarioId = "idle";
	private static long startedAtMs;
	private static long startedUsedBytes;
	private static final List<Long> tickSamplesNs = new ArrayList<>();
	private static final List<Long> frameSamplesNs = new ArrayList<>();

	private BenchHarness() {}

	public static synchronized void start(String scenario) {
		running = true;
		scenarioId = scenario == null ? "idle" : scenario;
		tickSamplesNs.clear();
		frameSamplesNs.clear();
		startedAtMs = System.currentTimeMillis();
		startedUsedBytes = usedMemory();
	}

	public static synchronized void stop() {
		running = false;
	}

	public static synchronized boolean isRunning() {
		return running;
	}

	public static synchronized void recordTickNanos(long nanos) {
		if (running && nanos > 0L && nanos < 5_000_000_000L) {
			tickSamplesNs.add(nanos);
		}
	}

	public static synchronized void recordFrameNanos(long nanos) {
		if (running && nanos > 0L && nanos < 5_000_000_000L) {
			frameSamplesNs.add(nanos);
		}
	}

	/** Copy of raw frame-time samples (ns) for the current/last run, in arrival order. */
	public static synchronized List<Long> frameSamplesCopy() {
		return new ArrayList<>(frameSamplesNs);
	}

	public static synchronized String report() {
		return snapshot().toString();
	}

	public static synchronized BenchMetrics snapshot() {
		BenchMetrics m = new BenchMetrics();
		m.scenarioId = scenarioId;
		m.memoryUsedBytes = usedMemory();
		if (tickSamplesNs.isEmpty() && frameSamplesNs.isEmpty()) {
			m.notes = running
					? "running but no samples yet"
					: "no samples collected; refuse to invent FPS or Sodium/Lithium comparisons";
			return m;
		}
		if (!tickSamplesNs.isEmpty()) {
			m.tickDurationMs = averageMs(tickSamplesNs);
		}
		if (!frameSamplesNs.isEmpty()) {
			List<Long> sorted = new ArrayList<>(frameSamplesNs);
			Collections.sort(sorted);
			m.frameTimeP50Ms = percentileMs(sorted, 0.50);
			m.frameTimeP99Ms = percentileMs(sorted, 0.99);
			double avgMs = averageMs(sorted);
			if (avgMs > 0.0) {
				m.avgFps = 1000.0 / avgMs;
			}
			// 1% low ≈ reciprocal of 99th percentile frame time
			if (m.frameTimeP99Ms != null && m.frameTimeP99Ms > 0.0) {
				m.pct1LowFps = 1000.0 / m.frameTimeP99Ms;
			}
			// 0.1% low ≈ reciprocal of 99.9th percentile frame time
			double p999Ms = percentileMs(sorted, 0.999);
			if (p999Ms > 0.0) {
				m.pct01LowFps = 1000.0 / p999Ms;
			}
		}
		long elapsedMs = Math.max(1L, System.currentTimeMillis() - startedAtMs);
		long delta = Math.max(0L, m.memoryUsedBytes - startedUsedBytes);
		m.allocRateBytesPerSec = (delta * 1000.0) / elapsedMs;
		m.notes = "ticks=" + tickSamplesNs.size() + " frames=" + frameSamplesNs.size()
				+ "; Sodium/Lithium columns intentionally empty (measure on a real PC — see docs/BENCH.md)";
		return m;
	}

	private static double averageMs(List<Long> nanos) {
		long sum = 0L;
		for (long n : nanos) sum += n;
		return (sum / (double) nanos.size()) / 1_000_000.0;
	}

	private static double percentileMs(List<Long> sortedNanos, double p) {
		if (sortedNanos.isEmpty()) return Double.NaN;
		int idx = (int) Math.min(sortedNanos.size() - 1, Math.max(0, Math.round(p * (sortedNanos.size() - 1))));
		return sortedNanos.get(idx) / 1_000_000.0;
	}

	private static long usedMemory() {
		Runtime rt = Runtime.getRuntime();
		return rt.totalMemory() - rt.freeMemory();
	}
}
