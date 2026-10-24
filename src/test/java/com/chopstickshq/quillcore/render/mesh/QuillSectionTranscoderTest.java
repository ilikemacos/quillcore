package com.chopstickshq.quillcore.render.mesh;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class QuillSectionTranscoderTest {
	private static final int S = QuillSectionTranscoder.SRC_STRIDE;

	/** Writes a vanilla BLOCK vertex (pos f32×3, color u8×4, uv0 f32×2, uv2 s16×2, normal s8×3+pad). */
	private static void putBlock(ByteBuffer b, int i, float x, float y, float z, int color, float u, float v, int lu, int lv, int nx, int ny, int nz) {
		int o = i * S;
		b.putFloat(o, x).putFloat(o + 4, y).putFloat(o + 8, z).putInt(o + 12, color)
				.putFloat(o + 16, u).putFloat(o + 20, v).putShort(o + 24, (short) lu).putShort(o + 26, (short) lv)
				.put(o + 28, (byte) nx).put(o + 29, (byte) ny).put(o + 30, (byte) nz).put(o + 31, (byte) 0);
	}

	private static ByteBuffer upQuad(int quads) {
		ByteBuffer b = ByteBuffer.allocateDirect(quads * 4 * S).order(ByteOrder.nativeOrder());
		for (int q = 0; q < quads; q++) {
			float x0 = q % 16, y = 5f;
			putBlock(b, q * 4, x0, y, 0, 0xFF7FBF3F + q, 0.25f, 0.5f, 240, 48, 0, 127, 0);
			putBlock(b, q * 4 + 1, x0, y, 1, 0xFF7FBF3F, 0.25f, 0.5625f, 240, 48, 0, 127, 0);
			putBlock(b, q * 4 + 2, x0 + 1, y, 1, 0xFF7FBF3F, 0.3125f, 0.5625f, 224, 32, 0, 127, 0);
			putBlock(b, q * 4 + 3, x0 + 1, y, 0, 0xFF7FBF3F, 0.3125f, 0.5f, 240, 48, 0, 127, 0);
		}
		return b;
	}

	@Test
	void transcodesAxisAlignedQuadsPreservingEveryField() {
		ByteBuffer b = upQuad(3);
		assertTrue(QuillSectionTranscoder.transcodeInPlace(b, 12));
		QuillCompactVertex v = QuillCompactVertex.decode(b, 2 * QuillCompactVertex.BYTES);
		assertEquals(1f, v.x(), 1e-3);
		assertEquals(5f, v.y(), 1e-3);
		assertEquals(1f, v.z(), 1e-3);
		assertEquals(2, v.face()); // +Y
		assertEquals(0.3125f, v.u(), 1e-4);
		assertEquals(0.5625f, v.v(), 1e-4);
		assertEquals(224, v.lightU());
		assertEquals(32, v.lightV());
		// color bytes are copied verbatim
		ByteBuffer ref = upQuad(3);
		assertEquals(ref.getInt(8 * S + 12), b.getInt(8 * QuillCompactVertex.BYTES + 8));
		QuillCompactVertex last = QuillCompactVertex.decode(b, 11 * QuillCompactVertex.BYTES);
		assertEquals(3f, last.x(), 1e-3);
	}

	@Test
	void anyNonAxisAlignedQuadLeavesSectionVanilla() {
		ByteBuffer b = upQuad(2);
		// tilt the second quad: diagonal normal
		for (int k = 4; k < 8; k++) b.put(k * S + 28, (byte) 90).put(k * S + 29, (byte) 90);
		ByteBuffer before = upQuad(2);
		for (int k = 4; k < 8; k++) before.put(k * S + 28, (byte) 90).put(k * S + 29, (byte) 90);
		assertFalse(QuillSectionTranscoder.transcodeInPlace(b, 8));
		assertEquals(before.rewind(), b.rewind());
	}

	@Test
	void nonPlanarQuadWithAxisNormalFallsBack() {
		ByteBuffer b = upQuad(1);
		b.putFloat(2 * S + 4, 5.5f);
		assertFalse(QuillSectionTranscoder.transcodeInPlace(b, 4));
	}

	@Test
	void outOfRangePositionFallsBack() {
		ByteBuffer b = upQuad(1);
		for (int k = 0; k < 4; k++) b.putFloat(k * S + 4, 30f);
		assertFalse(QuillSectionTranscoder.transcodeInPlace(b, 4));
	}

	@Test
	void rejectsPartialQuadsAndEmpty() {
		assertFalse(QuillSectionTranscoder.transcodeInPlace(upQuad(1), 3));
		assertFalse(QuillSectionTranscoder.transcodeInPlace(upQuad(1), 0));
	}

	@Test
	void faceFromNormalOnlyExactAxes() {
		assertEquals(0, QuillSectionTranscoder.faceFromNormal((byte) 127, (byte) 0, (byte) 0));
		assertEquals(5, QuillSectionTranscoder.faceFromNormal((byte) 0, (byte) 0, (byte) -127));
		assertEquals(-1, QuillSectionTranscoder.faceFromNormal((byte) 126, (byte) 10, (byte) 0));
	}

	@Test
	void builtBytesAreSixtyTwoPointFivePercent() {
		QuillMeshStats.reset();
		QuillMeshStats.recordSolid(400, true);
		QuillMeshStats.recordSolid(100, false);
		QuillMeshStats.Snapshot s = QuillMeshStats.snapshot();
		assertEquals(500L * 32, s.vanillaBytes());
		assertEquals(400L * 20 + 100L * 32, s.builtBytes());
		assertEquals(1, s.quillSections());
		assertEquals(1, s.fallbackSections());
	}
}
