package com.chopstickshq.quillcore.render.mesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Fixed 20-byte, 4-byte-aligned compact block vertex (unpacked form). Matches the GPU layout of the
 * {@code quillcore:pipeline/quill_solid} vertex format.
 *
 * <pre>
 * 0-7   position  4×s16: x,y,z = round((p + 8) * 2048) - 32768, section-local p in [-8, +24);
 *                 4th short = face 0..5 (+X,-X,+Y,-Y,+Z,-Z)
 * 8-11  color     4×u8 RGBA, copied verbatim (biome tint × ambient-occlusion shade)
 * 12-15 uv0       2×u16 (stored as s16 bit pattern), round(uv * 65536) capped at 65535
 * 16-19 uv2       2×s16 lightmap coords, copied verbatim
 * </pre>
 * Multi-byte fields are little-endian in {@link #encode()}; the transcoder writes in the buffer's own order.
 */
public record QuillCompactVertex(float x, float y, float z, int face, int colorRgba, float u, float v, short lightU, short lightV) {
	public static final int BYTES = 20;
	public static final float POS_MIN = -8f;
	public static final float POS_SCALE = 2048f;
	public static final float UV_SCALE = 65536f;

	/** Biased signed short for a section-local coordinate, or {@code Integer.MIN_VALUE} if it does not fit. */
	public static int quantizePos(float p) {
		if (!(p >= POS_MIN)) return Integer.MIN_VALUE; // also rejects NaN
		long q = Math.round(((double) p - POS_MIN) * POS_SCALE) - 32768L;
		return q > Short.MAX_VALUE ? Integer.MIN_VALUE : (int) q;
	}

	/** Unsigned 16-bit texture coordinate, or -1 if outside [0, 1]. */
	public static int quantizeUv(float t) {
		if (!(t >= 0f && t <= 1f)) return -1;
		return (int) Math.min(0xFFFF, Math.round((double) t * UV_SCALE));
	}

	public static float dequantizePos(int s) {
		return (s + 32768) / POS_SCALE + POS_MIN;
	}

	public static float dequantizeUv(int raw) {
		return (raw & 0xFFFF) / UV_SCALE;
	}

	/** Writes one vertex at {@code buf}'s absolute index {@code off} in the buffer's byte order. Caller validates ranges. */
	public static void put(ByteBuffer buf, int off, int qx, int qy, int qz, int face, int colorRaw, int qu, int qv, short lightU, short lightV) {
		buf.putShort(off, (short) qx);
		buf.putShort(off + 2, (short) qy);
		buf.putShort(off + 4, (short) qz);
		buf.putShort(off + 6, (short) face);
		buf.putInt(off + 8, colorRaw);
		buf.putShort(off + 12, (short) qu);
		buf.putShort(off + 14, (short) qv);
		buf.putShort(off + 16, lightU);
		buf.putShort(off + 18, lightV);
	}

	/** Little-endian 20-byte encoding; throws if any field is out of range. */
	public byte[] encode() {
		if (face < 0 || face > 5) throw new IllegalArgumentException("face must be 0..5: " + face);
		int qx = quantizePos(x), qy = quantizePos(y), qz = quantizePos(z);
		if (qx == Integer.MIN_VALUE || qy == Integer.MIN_VALUE || qz == Integer.MIN_VALUE) {
			throw new IllegalArgumentException("position out of [-8, 24): " + x + "," + y + "," + z);
		}
		int qu = quantizeUv(u), qv = quantizeUv(v);
		if (qu < 0 || qv < 0) throw new IllegalArgumentException("uv out of [0, 1]: " + u + "," + v);
		ByteBuffer b = ByteBuffer.allocate(BYTES).order(ByteOrder.LITTLE_ENDIAN);
		put(b, 0, qx, qy, qz, face, Integer.reverseBytes(colorRgba), qu, qv, lightU, lightV);
		return b.array();
	}

	public static QuillCompactVertex decode(byte[] bytes) {
		if (bytes.length != BYTES) throw new IllegalArgumentException("expected " + BYTES + " bytes, got " + bytes.length);
		return decode(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN), 0);
	}

	/** Decodes the vertex at absolute index {@code off}; color bytes are returned as RGBA (R in the high byte). */
	public static QuillCompactVertex decode(ByteBuffer b, int off) {
		int colorRaw = b.getInt(off + 8);
		int rgba = b.order() == ByteOrder.LITTLE_ENDIAN ? Integer.reverseBytes(colorRaw) : colorRaw;
		return new QuillCompactVertex(
				dequantizePos(b.getShort(off)), dequantizePos(b.getShort(off + 2)), dequantizePos(b.getShort(off + 4)),
				b.getShort(off + 6), rgba,
				dequantizeUv(b.getShort(off + 12)), dequantizeUv(b.getShort(off + 14)),
				b.getShort(off + 16), b.getShort(off + 18));
	}
}
