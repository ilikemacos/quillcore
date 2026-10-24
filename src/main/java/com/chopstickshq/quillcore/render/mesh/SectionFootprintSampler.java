package com.chopstickshq.quillcore.render.mesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Compares vanilla and compact byte size for a list of vertices by encoding each one.
 * Used by tests. Live sections are counted in {@link QuillMeshStats}.
 */
public final class SectionFootprintSampler {
	public record Footprint(long vertexCount, long vanillaBytes, long quillBytes, double reductionPct) {}

	private SectionFootprintSampler() {}

	public static Footprint sample(List<QuillCompactVertex> vertices) {
		long quill = 0;
		for (QuillCompactVertex v : vertices) quill += v.encode().length;
		long n = vertices.size();
		long vanilla = VanillaVertexFootprint.bytesFor(n);
		double pct = vanilla == 0 ? 0.0 : (1.0 - (double) quill / vanilla) * 100.0;
		return new Footprint(n, vanilla, quill, pct);
	}

	/** Representative sample: the unculled outer shell of a fully solid 16³ section (6 faces × 256 quads × 4 verts). */
	public static Footprint sampleSolidSectionShell() {
		return sample(solidSectionShell());
	}

	public static List<QuillCompactVertex> solidSectionShell() {
		List<QuillCompactVertex> out = new ArrayList<>(6 * 256 * 4);
		for (int face = 0; face < 6; face++) {
			int axis = face / 2;
			float fixed = face % 2 == 0 ? 16f : 0f;
			for (int a = 0; a < 16; a++) {
				for (int b = 0; b < 16; b++) {
					for (int corner = 0; corner < 4; corner++) {
						float da = a + (corner == 1 || corner == 2 ? 1 : 0);
						float db = b + (corner >= 2 ? 1 : 0);
						// (fixed, da, db) rotated so the fixed coordinate lies on this face's axis
						float x = axis == 0 ? fixed : da;
						float y = axis == 1 ? fixed : (axis == 0 ? da : db);
						float z = axis == 2 ? fixed : db;
						out.add(new QuillCompactVertex(x, y, z, face, 0xFFFFFFFF, da / 16f, db / 16f, (short) 0, (short) 240));
					}
				}
			}
		}
		return out;
	}
}
