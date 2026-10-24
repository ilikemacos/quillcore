package com.chopstickshq.quillcore.bench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Serialises measured {@link BenchMetrics} segments (QuillCore on / off) to JSON + CSV.
 * Pure Java, no Minecraft imports. Missing values are written as null / empty — never invented.
 */
public final class BenchReport {
	public static final String CSV_HEADER = "segment,label,avgFps,pct1LowFps,frameTimeP50Ms,frameTimeP99Ms,msptAvg,memoryUsedBytes,pct01LowFps";

	private final List<String> labels = new ArrayList<>();
	private final List<BenchMetrics> segments = new ArrayList<>();
	private final List<List<Double>> fpsSeries = new ArrayList<>();
	private final List<List<Double>> frameMsSeries = new ArrayList<>();
	private String environment = "";

	public BenchReport environment(String env) {
		this.environment = env == null ? "" : env;
		return this;
	}

	public void add(String label, BenchMetrics m) {
		add(label, m, List.of());
	}

	/** Adds a segment plus its raw frame-time samples (ns), reduced to per-second FPS and ~300-point frame-time series. */
	public void add(String label, BenchMetrics m, List<Long> frameNs) {
		labels.add(label);
		segments.add(m);
		fpsSeries.add(fpsPerSecond(frameNs));
		frameMsSeries.add(downsampleMs(frameNs, 300));
	}

	/** FPS for each whole elapsed second, computed from consecutive frame durations. Partial last second dropped. */
	public static List<Double> fpsPerSecond(List<Long> frameNs) {
		List<Double> out = new ArrayList<>();
		long acc = 0;
		int frames = 0;
		for (long ns : frameNs) {
			acc += ns;
			frames++;
			if (acc >= 1_000_000_000L) {
				out.add(frames * 1e9 / acc);
				acc = 0;
				frames = 0;
			}
		}
		return out;
	}

	/** Max-of-bucket downsample (keeps stutter spikes visible), in ms. */
	public static List<Double> downsampleMs(List<Long> frameNs, int points) {
		List<Double> out = new ArrayList<>();
		if (frameNs.isEmpty() || points <= 0) return out;
		int bucket = Math.max(1, (int) Math.ceil(frameNs.size() / (double) points));
		for (int i = 0; i < frameNs.size(); i += bucket) {
			long max = 0;
			for (int j = i; j < Math.min(frameNs.size(), i + bucket); j++) max = Math.max(max, frameNs.get(j));
			out.add(max / 1e6);
		}
		return out;
	}

	public int size() {
		return segments.size();
	}

	/** Mean of a metric over all segments with the given label; null when no segment measured it. */
	public Double mean(String label, java.util.function.Function<BenchMetrics, Double> field) {
		double sum = 0;
		int n = 0;
		for (int i = 0; i < segments.size(); i++) {
			if (!labels.get(i).equals(label)) continue;
			Double v = field.apply(segments.get(i));
			if (v != null && !v.isNaN() && !v.isInfinite()) {
				sum += v;
				n++;
			}
		}
		return n == 0 ? null : sum / n;
	}

	/** Percent change of b relative to a, or null if either is missing / a is zero. */
	public static Double percentDelta(Double a, Double b) {
		if (a == null || b == null || a == 0.0) return null;
		return (b - a) / a * 100.0;
	}

	public String toCsv() {
		StringBuilder sb = new StringBuilder(CSV_HEADER).append('\n');
		for (int i = 0; i < segments.size(); i++) {
			BenchMetrics m = segments.get(i);
			sb.append(i).append(',').append(labels.get(i)).append(',')
					.append(csv(m.avgFps)).append(',').append(csv(m.pct1LowFps)).append(',')
					.append(csv(m.frameTimeP50Ms)).append(',').append(csv(m.frameTimeP99Ms)).append(',')
					.append(csv(m.tickDurationMs)).append(',')
					.append(m.memoryUsedBytes == null ? "" : m.memoryUsedBytes.toString()).append(',')
					.append(csv(m.pct01LowFps)).append('\n');
		}
		return sb.toString();
	}

	public String toJson() {
		StringBuilder sb = new StringBuilder("{\n");
		sb.append("  \"environment\": ").append(str(environment)).append(",\n");
		sb.append("  \"segments\": [\n");
		for (int i = 0; i < segments.size(); i++) {
			BenchMetrics m = segments.get(i);
			sb.append("    {\"segment\": ").append(i)
					.append(", \"label\": ").append(str(labels.get(i)))
					.append(", \"avgFps\": ").append(num(m.avgFps))
					.append(", \"pct1LowFps\": ").append(num(m.pct1LowFps))
					.append(", \"pct01LowFps\": ").append(num(m.pct01LowFps))
					.append(", \"frameTimeP50Ms\": ").append(num(m.frameTimeP50Ms))
					.append(", \"frameTimeP99Ms\": ").append(num(m.frameTimeP99Ms))
					.append(", \"msptAvg\": ").append(num(m.tickDurationMs))
					.append(", \"notes\": ").append(str(m.notes))
					.append(", \"fpsPerSecond\": ").append(arr(fpsSeries.get(i)))
					.append(", \"frameTimeMsSeries\": ").append(arr(frameMsSeries.get(i)))
					.append('}').append(i + 1 < segments.size() ? "," : "").append('\n');
		}
		sb.append("  ],\n  \"summary\": {\n");
		String[] metrics = {"avgFps", "pct1LowFps", "pct01LowFps", "frameTimeP50Ms", "frameTimeP99Ms", "msptAvg"};
		for (int k = 0; k < metrics.length; k++) {
			java.util.function.Function<BenchMetrics, Double> f = field(metrics[k]);
			Double on = mean("on", f), off = mean("off", f);
			sb.append("    \"").append(metrics[k]).append("\": {\"on\": ").append(num(on))
					.append(", \"off\": ").append(num(off))
					.append(", \"onVsOffPct\": ").append(num(percentDelta(off, on))).append('}')
					.append(k + 1 < metrics.length ? "," : "").append('\n');
		}
		sb.append("  }\n}\n");
		return sb.toString();
	}

	public void write(Path dir) throws IOException {
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("bench.json"), toJson(), StandardCharsets.UTF_8);
		Files.writeString(dir.resolve("bench.csv"), toCsv(), StandardCharsets.UTF_8);
	}

	static java.util.function.Function<BenchMetrics, Double> field(String name) {
		return switch (name) {
			case "avgFps" -> m -> m.avgFps;
			case "pct1LowFps" -> m -> m.pct1LowFps;
			case "pct01LowFps" -> m -> m.pct01LowFps;
			case "frameTimeP50Ms" -> m -> m.frameTimeP50Ms;
			case "frameTimeP99Ms" -> m -> m.frameTimeP99Ms;
			case "msptAvg" -> m -> m.tickDurationMs;
			default -> throw new IllegalArgumentException(name);
		};
	}

	private static String arr(List<Double> xs) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < xs.size(); i++) sb.append(i > 0 ? "," : "").append(num(xs.get(i)));
		return sb.append(']').toString();
	}

	private static String csv(Double d) {
		return d == null || d.isNaN() ? "" : String.format(Locale.ROOT, "%.3f", d);
	}

	private static String num(Double d) {
		return d == null || d.isNaN() || d.isInfinite() ? "null" : String.format(Locale.ROOT, "%.3f", d);
	}

	private static String str(String s) {
		if (s == null) return "null";
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
	}
}
