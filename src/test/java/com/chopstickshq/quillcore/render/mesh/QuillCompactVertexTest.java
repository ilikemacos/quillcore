package com.chopstickshq.quillcore.render.mesh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuillCompactVertexTest {
	private static final float POS_EPS = 1f / 4096f;
	private static final float UV_EPS = 1f / 65535f;

	private static QuillCompactVertex rt(float x, float y, float z, int face, float u, float v, int bl, int sl) {
		byte[] b = QuillCompactVertex.encode(x, y, z, face, u, v, bl, sl);
		assertEquals(QuillCompactVertex.BYTES, b.length);
		return QuillCompactVertex.decode(b);
	}

	@Test
	void positionCornersAndMid() {
		QuillCompactVertex o = rt(0, 0, 0, 0, 0, 0, 0, 0);
		assertEquals(0f, o.x());
		assertEquals(0f, o.y());
		assertEquals(0f, o.z());
		QuillCompactVertex max = rt(16, 16, 16, 0, 0, 0, 0, 0);
		assertEquals(16f, max.x(), POS_EPS);
		assertEquals(16f, max.y(), POS_EPS);
		assertEquals(16f, max.z(), POS_EPS);
		QuillCompactVertex mid = rt(7.3f, 8.5f, 0.125f, 0, 0, 0, 0, 0);
		assertEquals(7.3f, mid.x(), POS_EPS);
		assertEquals(8.5f, mid.y(), POS_EPS);
		assertEquals(0.125f, mid.z(), POS_EPS);
	}

	@Test
	void positionOutOfRangeClamped() {
		QuillCompactVertex q = rt(-3, 40, Float.NaN, 0, 0, 0, 0, 0);
		assertEquals(0f, q.x());
		assertEquals(16f, q.y(), POS_EPS);
		assertEquals(0f, q.z());
	}

	@Test
	void uvRoundTrip() {
		assertEquals(0f, rt(0, 0, 0, 0, 0f, 0f, 0, 0).u());
		QuillCompactVertex one = rt(0, 0, 0, 0, 1f, 1f, 0, 0);
		assertEquals(1f, one.u());
		assertEquals(1f, one.v());
		QuillCompactVertex half = rt(0, 0, 0, 0, 0.5f, 0.5f, 0, 0);
		assertEquals(0.5f, half.u(), UV_EPS);
		assertEquals(0.5f, half.v(), UV_EPS);
	}

	@Test
	void lightBoundsAndClamp() {
		QuillCompactVertex a = rt(0, 0, 0, 0, 0, 0, 0, 15);
		assertEquals(0, a.blockLight());
		assertEquals(15, a.skyLight());
		QuillCompactVertex c = rt(0, 0, 0, 0, 0, 0, -4, 99);
		assertEquals(0, c.blockLight());
		assertEquals(15, c.skyLight());
	}

	@Test
	void allAxisNormalsRoundTrip() {
		for (int f = 0; f < 6; f++) {
			QuillCompactVertex q = rt(1, 1, 1, f, 0, 0, 0, 0);
			assertEquals(f, q.face());
			assertTrue(q.isAxisAligned());
		}
	}

	@Test
	void sentinelDecodesAsNonAxisAligned() {
		QuillCompactVertex q = rt(1, 1, 1, QuillCompactVertex.NON_AXIS_ALIGNED, 0, 0, 0, 0);
		assertEquals(QuillCompactVertex.NON_AXIS_ALIGNED, q.face());
		assertFalse(q.isAxisAligned());
	}

	@Test
	void invalidFaceRejected() {
		assertThrows(IllegalArgumentException.class, () -> QuillCompactVertex.encode(0, 0, 0, 6, 0, 0, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> QuillCompactVertex.encode(0, 0, 0, -1, 0, 0, 0, 0));
	}
}
