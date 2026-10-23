package com.chopstickshq.quillcore.bench;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory tick and frame time sampler.
 */
public final class BenchHarness {
	private static boolean running;
	private static String scenarioId = "idle";
	private static long startedAtMs;
	private static long startedUsedBytes;
	private static final List<Long> tickSamplesNs = new ArrayList<>();

	private BenchHarness() {}

	public static synchronized void start(String scenario) {
		running = true;
		scenarioId = scenario == null ? "idle" : scenario;
		tickSamplesNs.clear();
		startedAtMs = System.currentTimeMillis();
		startedUsedBytes = usedMemory();
	}

	public static synchronized void stop() {
		running = false;
	}

	public static synchronized boolean isRunning() {
		return running;
	}

	/** Optional: record a server tick duration sample while a bench is running. */
	public static synchronized void recordTickNanos(long nanos) {
		if (running) {
			tickSamplesNs.add(nanos);
		}
	}

	public static synchronized String report() {
		BenchMetrics m = snapshot();
		return m.toString();
	}

	public static synchronized BenchMetrics snapshot() {
		BenchMetrics m = new BenchMetrics();
		m.scenarioId = scenarioId;
		if (tickSamplesNs.isEmpty()) {
			m.notes = running
					? "running but no tick samples yet; FPS fields intentionally empty on headless"
					: "no samples collected";
			m.memoryUsedBytes = usedMemory();
			return m;
		}
		long sum = 0;
		for (long n : tickSamplesNs) sum += n;
		m.tickDurationMs = (sum / (double) tickSamplesNs.size()) / 1_000_000.0;
		m.memoryUsedBytes = usedMemory();
		long elapsedMs = Math.max(1L, System.currentTimeMillis() - startedAtMs);
		long delta = Math.max(0L, m.memoryUsedBytes - startedUsedBytes);
		m.allocRateBytesPerSec = (delta * 1000.0) / elapsedMs;
		m.notes = "tick samples=" + tickSamplesNs.size() + "; avg/1%low FPS not measured (no frame clock hooked in 0.1.0)";
		return m;
	}

	private static long usedMemory() {
		Runtime rt = Runtime.getRuntime();
		return rt.totalMemory() - rt.freeMemory();
	}
}
