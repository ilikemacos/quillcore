package com.chopstickshq.quillcore.render.mesh;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Real SOLID-layer section build counters, written from chunk-compile worker threads.
 * {@code vanillaBytes} is what vanilla would have built (vertices × 32); {@code builtBytes} is what was
 * actually handed to the GPU upload (×20 for Quill sections, ×32 for fallback/disabled ones).
 */
public final class QuillMeshStats {
	private static final AtomicLong QUILL_SECTIONS = new AtomicLong();
	private static final AtomicLong FALLBACK_SECTIONS = new AtomicLong();
	private static final AtomicLong VANILLA_BYTES = new AtomicLong();
	private static final AtomicLong BUILT_BYTES = new AtomicLong();

	public record Snapshot(long quillSections, long fallbackSections, long vanillaBytes, long builtBytes) {
		public double reductionPct() {
			return vanillaBytes == 0 ? 0.0 : (1.0 - (double) builtBytes / vanillaBytes) * 100.0;
		}
	}

	private QuillMeshStats() {}

	/** @param quill true if the section's SOLID mesh was transcoded; false if it stayed vanilla */
	public static void recordSolid(int vertexCount, boolean quill) {
		(quill ? QUILL_SECTIONS : FALLBACK_SECTIONS).incrementAndGet();
		VANILLA_BYTES.addAndGet(VanillaVertexFootprint.bytesFor(vertexCount));
		BUILT_BYTES.addAndGet((long) vertexCount * (quill ? QuillCompactVertex.BYTES : VanillaVertexFootprint.BLOCK_BYTES_PER_VERTEX));
	}

	public static void reset() {
		QUILL_SECTIONS.set(0);
		FALLBACK_SECTIONS.set(0);
		VANILLA_BYTES.set(0);
		BUILT_BYTES.set(0);
	}

	public static Snapshot snapshot() {
		return new Snapshot(QUILL_SECTIONS.get(), FALLBACK_SECTIONS.get(), VANILLA_BYTES.get(), BUILT_BYTES.get());
	}
}
