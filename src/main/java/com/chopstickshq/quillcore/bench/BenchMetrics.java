package com.chopstickshq.quillcore.bench;

/** Bench results. Fields stay null until there are samples. */
public final class BenchMetrics {
	public String scenarioId;
	public Double avgFps;
	public Double pct1LowFps;
	public Double pct01LowFps;
	public Double frameTimeP50Ms;
	public Double frameTimeP99Ms;
	public Double tickDurationMs;
	public Long memoryUsedBytes;
	public Double allocRateBytesPerSec;
	/** Vertex bytes for a sample section, not live meshes. */
	public Long vanillaVertexBytes;
	public Long quillVertexBytes;
	public Double vertexByteReductionPct;
	public String notes = "unmeasured";

	public boolean hasMeasurements() {
		return avgFps != null || pct1LowFps != null || tickDurationMs != null || memoryUsedBytes != null;
	}

	@Override
	public String toString() {
		if (!hasMeasurements()) {
			return "scenario=" + scenarioId + " status=NO_SAMPLES notes=" + notes;
		}
		return "scenario=" + scenarioId
				+ " avgFps=" + avgFps
				+ " pct1Low=" + pct1LowFps
				+ " pct01Low=" + pct01LowFps
				+ " ftP50Ms=" + frameTimeP50Ms
				+ " ftP99Ms=" + frameTimeP99Ms
				+ " tickMs=" + tickDurationMs
				+ " mem=" + memoryUsedBytes
				+ " allocBps=" + allocRateBytesPerSec
				+ " vanillaVtxBytes=" + vanillaVertexBytes
				+ " quillVtxBytes=" + quillVertexBytes
				+ " vtxReductionPct=" + vertexByteReductionPct
				+ " notes=" + notes;
	}
}
