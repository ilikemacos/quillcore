package com.chopstickshq.quillcore.bench;

import java.util.LinkedHashMap;
import java.util.Map;

/** Run-wide GPU time per render pass, fed by the client's timer queries. */
public final class GpuPassTimes {
	private static final Map<String, double[]> TOTALS = new LinkedHashMap<>();

	private GpuPassTimes() {}

	public static synchronized void add(String pass, double ms) {
		double[] t = TOTALS.computeIfAbsent(pass, k -> new double[2]);
		t[0] += ms;
		t[1]++;
	}

	/** Mean ms per pass, in first-seen order. */
	public static synchronized Map<String, Double> averages() {
		Map<String, Double> out = new LinkedHashMap<>();
		TOTALS.forEach((k, t) -> out.put(k, t[0] / t[1]));
		return out;
	}

	public static synchronized void clear() {
		TOTALS.clear();
	}
}
