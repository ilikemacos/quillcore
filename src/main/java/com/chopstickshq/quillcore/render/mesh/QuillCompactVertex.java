package com.chopstickshq.quillcore.render.mesh;

/**
 * Fixed 13-byte compact block vertex (unpacked form).
 *
 * <pre>
 * 0-5   position  3×u16, section-local, round(clamp(p,0,16)*4096), capped at 65535
 * 6     face      0..5 = +X,-X,+Y,-Y,+Z,-Z; 255 = non-axis-aligned
 * 7-10  uv        2×u16, round(clamp(uv,0,1)*65535)
 * 11    blockLight u8 0..15
 * 12    skyLight   u8 0..15
 * </pre>
 * Multi-byte fields are little-endian.
 */
public record QuillCompactVertex(float x, float y, float z, int face, float u, float v, int blockLight, int skyLight) {
	public static final int BYTES = 13;
	public static final int NON_AXIS_ALIGNED = 255;
	public static final float POS_SCALE = 4096f;
	public static final float UV_SCALE = 65535f;

	public boolean isAxisAligned() {
		return face != NON_AXIS_ALIGNED;
	}

	public static byte[] encode(float x, float y, float z, int face, float u, float v, int blockLight, int skyLight) {
		if (face != NON_AXIS_ALIGNED && (face < 0 || face > 5)) {
			throw new IllegalArgumentException("face must be 0..5 or 255: " + face);
		}
		byte[] out = new byte[BYTES];
		putU16(out, 0, quantize(x, 16f, POS_SCALE));
		putU16(out, 2, quantize(y, 16f, POS_SCALE));
		putU16(out, 4, quantize(z, 16f, POS_SCALE));
		out[6] = (byte) face;
		putU16(out, 7, quantize(u, 1f, UV_SCALE));
		putU16(out, 9, quantize(v, 1f, UV_SCALE));
		out[11] = (byte) clampLight(blockLight);
		out[12] = (byte) clampLight(skyLight);
		return out;
	}

	public byte[] encode() {
		return encode(x, y, z, face, u, v, blockLight, skyLight);
	}

	public static QuillCompactVertex decode(byte[] b) {
		if (b.length != BYTES) throw new IllegalArgumentException("expected " + BYTES + " bytes, got " + b.length);
		return new QuillCompactVertex(
				getU16(b, 0) / POS_SCALE, getU16(b, 2) / POS_SCALE, getU16(b, 4) / POS_SCALE,
				b[6] & 0xFF,
				getU16(b, 7) / UV_SCALE, getU16(b, 9) / UV_SCALE,
				b[11] & 0xFF, b[12] & 0xFF);
	}

	static int clampLight(int l) {
		return Math.max(0, Math.min(15, l));
	}

	private static int quantize(float value, float max, float scale) {
		float c = Float.isNaN(value) ? 0f : Math.max(0f, Math.min(max, value));
		return Math.min(0xFFFF, Math.round(c * scale));
	}

	private static void putU16(byte[] b, int off, int val) {
		b[off] = (byte) val;
		b[off + 1] = (byte) (val >>> 8);
	}

	private static int getU16(byte[] b, int off) {
		return (b[off] & 0xFF) | (b[off + 1] & 0xFF) << 8;
	}
}
