package com.chopstickshq.quillcore.csr.client;

import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.bench.GpuPassTimes;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GL_TIME_ELAPSED per pass. Each pass owns two queries and alternates between them, so a result
 * is read one use later and only if the driver says it is ready; we never wait on the GPU.
 */
final class CsrTimers {
	private static final long LOG_EVERY_NS = 5_000_000_000L;

	private static final class Pass {
		final int[] q = new int[2];
		final boolean[] pending = new boolean[2];
		int slot;
		double sumMs;
		int n;
	}

	private static final Map<String, Pass> PASSES = new LinkedHashMap<>();
	private static Pass active;
	private static long windowStart;
	private static boolean warnedBusy;

	private CsrTimers() {}

	static void begin(String name) {
		if (!CsrGl.timersAvailable() || active != null) {
			return;
		}
		// Someone else (vanilla's F3 profiler, another mod) may own the TIME_ELAPSED target right now.
		int busy = GL15.glGetQueryi(GL33.GL_TIME_ELAPSED, GL15.GL_CURRENT_QUERY);
		if (busy != 0) {
			if (!warnedBusy) {
				warnedBusy = true;
				QuillCore.LOGGER.info("CSR timers: GL_TIME_ELAPSED busy (query {}), skipping samples while it is", busy);
			}
			return;
		}
		Pass p = PASSES.computeIfAbsent(name, k -> {
			Pass n = new Pass();
			GL15.glGenQueries(n.q);
			return n;
		});
		p.slot ^= 1;
		int q = p.q[p.slot];
		if (p.pending[p.slot]) {
			if (GL15.glGetQueryObjecti(q, GL15.GL_QUERY_RESULT_AVAILABLE) == 0) {
				return; // still in flight: skip this sample rather than stall
			}
			double ms = GL33.glGetQueryObjecti64(q, GL15.GL_QUERY_RESULT) / 1e6;
			p.sumMs += ms;
			p.n++;
			GpuPassTimes.add(name, ms);
		}
		GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, q);
		p.pending[p.slot] = true;
		active = p;
	}

	static void end() {
		if (active == null) {
			return;
		}
		GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
		active = null;
		long now = System.nanoTime();
		if (windowStart == 0) {
			windowStart = now;
		} else if (now - windowStart >= LOG_EVERY_NS) {
			windowStart = now;
			log();
		}
	}

	private static void log() {
		StringBuilder sb = new StringBuilder("CSR GPU ms/pass (5s avg):");
		double total = 0;
		for (Map.Entry<String, Pass> e : PASSES.entrySet()) {
			Pass p = e.getValue();
			if (p.n == 0) {
				continue;
			}
			double avg = p.sumMs / p.n;
			total += avg;
			sb.append(' ').append(e.getKey()).append('=').append(String.format(java.util.Locale.ROOT, "%.3f", avg));
			p.sumMs = 0;
			p.n = 0;
		}
		sb.append(String.format(java.util.Locale.ROOT, " total=%.3f", total));
		QuillCore.LOGGER.info(sb.toString());
	}

	/** Drop all queries; call when the CSR GL objects are torn down. */
	static void close() {
		if (active != null) {
			GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
			active = null;
		}
		for (Pass p : PASSES.values()) {
			GL15.glDeleteQueries(p.q);
		}
		PASSES.clear();
		windowStart = 0;
	}
}
