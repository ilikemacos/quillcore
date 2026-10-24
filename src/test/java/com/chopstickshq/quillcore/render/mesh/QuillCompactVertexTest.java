package com.chopstickshq.quillcore.render.mesh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuillCompactVertexTest {
	private static final float POS_EPS = 0.5f / QuillCompactVertex.POS_SCALE;
	private static final float UV_EPS = 1f / 65535f;

	private static QuillCompactVertex rt(QuillCompactVertex v) {
		byte[] b = v.encode();
		assertEquals(QuillCompactVertex.BYTES, b.length);
		return QuillCompactVertex.decode(b);
	}

	@Test
	void layoutIsTwentyBytes() {
		assertEquals(20, QuillCompactVertex.BYTES);
	}

	@Test
	void roundTripsAllFields() {
		QuillCompactVertex in = new QuillCompactVertex(7.3f, 16f, -0.25f, 4, 0x11223344, 0.123456f, 0.9f, (short) 240, (short) 176);
		QuillCompactVertex out = rt(in);
		assertEquals(in.x(), out.x(), POS_EPS);
		assertEquals(in.y(), out.y(), POS_EPS);
		assertEquals(in.z(), out.z(), POS_EPS);
		assertEquals(4, out.face());
		assertEquals(0x11223344, out.colorRgba());
		assertEquals(in.u(), out.u(), UV_EPS);
		assertEquals(in.v(), out.v(), UV_EPS);
		assertEquals(240, out.lightU());
		assertEquals(176, out.lightV());
	}

	@Test
	void signedBiasCoversOverhangRange() {
		assertEquals(Short.MIN_VALUE, QuillCompactVertex.quantizePos(-8f));
		assertEquals(0f, QuillCompactVertex.dequantizePos(QuillCompactVertex.quantizePos(0f)));
		assertEquals(16f, QuillCompactVertex.dequantizePos(QuillCompactVertex.quantizePos(16f)));
		assertEquals(23.999f, QuillCompactVertex.dequantizePos(QuillCompactVertex.quantizePos(23.999f)), POS_EPS);
		assertEquals(Integer.MIN_VALUE, QuillCompactVertex.quantizePos(24f));
		assertEquals(Integer.MIN_VALUE, QuillCompactVertex.quantizePos(-8.01f));
		assertEquals(Integer.MIN_VALUE, QuillCompactVertex.quantizePos(Float.NaN));
	}

	@Test
	void uvRangeChecked() {
		assertEquals(0, QuillCompactVertex.quantizeUv(0f));
		assertEquals(0xFFFF, QuillCompactVertex.quantizeUv(1f));
		assertEquals(-1, QuillCompactVertex.quantizeUv(1.01f));
		assertEquals(-1, QuillCompactVertex.quantizeUv(-0.01f));
	}

	@Test
	void encodeRejectsOutOfRange() {
		assertThrows(IllegalArgumentException.class, () -> new QuillCompactVertex(30f, 0, 0, 0, 0, 0, 0, (short) 0, (short) 0).encode());
		assertThrows(IllegalArgumentException.class, () -> new QuillCompactVertex(0, 0, 0, 6, 0, 0, 0, (short) 0, (short) 0).encode());
	}
}
