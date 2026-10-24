package com.chopstickshq.quillcore.render.mesh;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SectionFootprintSamplerTest {
	@Test
	void solidCubeShellFootprint() {
		SectionFootprintSampler.Footprint f = SectionFootprintSampler.sampleSolidSectionShell();
		long verts = 6L * 256 * 4;
		assertEquals(verts, f.vertexCount());
		assertEquals(verts * VanillaVertexFootprint.BLOCK_BYTES_PER_VERTEX, f.vanillaBytes());
		assertEquals(verts * QuillCompactVertex.BYTES, f.quillBytes());
		assertEquals((1.0 - (double) f.quillBytes() / f.vanillaBytes()) * 100.0, f.reductionPct(), 1e-9);
		assertTrue(f.reductionPct() > 0);
	}

	@Test
	void shellVerticesAreInRangeAndOnTheirFace() {
		for (QuillCompactVertex v : SectionFootprintSampler.solidSectionShell()) {
			float fixed = switch (v.face() / 2) { case 0 -> v.x(); case 1 -> v.y(); default -> v.z(); };
			assertEquals(v.face() % 2 == 0 ? 16f : 0f, fixed);
			for (float c : new float[]{v.x(), v.y(), v.z()}) assertTrue(c >= 0 && c <= 16);
		}
	}

	@Test
	void emptyInput() {
		SectionFootprintSampler.Footprint f = SectionFootprintSampler.sample(List.of());
		assertEquals(0, f.vertexCount());
		assertEquals(0.0, f.reductionPct());
	}
}
