package com.chopstickshq.quillcore.render.mesh;

import java.nio.ByteBuffer;

/**
 * Re-encodes a built vanilla {@code BLOCK}-format quad mesh (32 B/vertex) into {@link QuillCompactVertex}
 * (20 B/vertex) in place. All-or-nothing: the whole mesh is validated first, and if any quad is not
 * axis-aligned, any position falls outside the encodable range, or anything else is unexpected, the
 * buffer is left untouched and the caller keeps the vanilla mesh.
 *
 * <p>In-place is safe because the output stride is smaller than the input stride: vertex {@code i} is
 * read fully before its 20 bytes are written at {@code i*20}, which never reaches vertex {@code i+1}'s
 * source bytes at {@code (i+1)*32}.
 */
public final class QuillSectionTranscoder {
	/** {@code DefaultVertexFormat.BLOCK} layout (see {@link VanillaVertexFootprint}). */
	public static final int SRC_STRIDE = VanillaVertexFootprint.BLOCK_BYTES_PER_VERTEX;
	static final int SRC_POS = 0, SRC_COLOR = 12, SRC_UV0 = 16, SRC_UV2 = 24, SRC_NORMAL = 28;
	/** Vertices of one quad must agree on the face-axis coordinate to within this (blocks). */
	static final float PLANE_EPS = 1e-4f;

	private QuillSectionTranscoder() {}

	/**
	 * @param buf vanilla vertex data starting at {@code buf.position()}, in the buffer's byte order
	 * @return true if {@code vertexCount * 20} bytes at {@code buf.position()} now hold the Quill encoding;
	 *         false if the mesh must stay vanilla (buffer unchanged)
	 */
	public static boolean transcodeInPlace(ByteBuffer buf, int vertexCount) {
		return transcodeInPlace(buf, vertexCount, 0);
	}

	/**
	 * As {@link #transcodeInPlace(ByteBuffer, int)}, tagging every vertex with the section's slot in its region
	 * (0..255, see {@code RegionPos.slot}) so the shader can place it relative to the region origin.
	 */
	public static boolean transcodeInPlace(ByteBuffer buf, int vertexCount, int slot) {
		if (slot < 0 || slot > QuillCompactVertex.MAX_SLOT) throw new IllegalArgumentException("slot " + slot);
		if (vertexCount <= 0 || vertexCount % 4 != 0) return false;
		int base = buf.position();
		if ((long) vertexCount * SRC_STRIDE > buf.remaining()) return false;
		int quads = vertexCount / 4;
		byte[] faces = new byte[quads];
		for (int q = 0; q < quads; q++) {
			int face = validateQuad(buf, base + q * 4 * SRC_STRIDE);
			if (face < 0) return false;
			faces[q] = (byte) face;
		}
		for (int i = 0; i < vertexCount; i++) {
			writeVertex(buf, base + i * SRC_STRIDE, buf, base + i * QuillCompactVertex.BYTES, faces[i / 4], slot);
		}
		return true;
	}

	/**
	 * As {@link #transcodeInPlace(ByteBuffer, int, int)}, but the quads come out grouped by face in the order
	 * +X,-X,+Y,-Y,+Z,-Z so a draw can skip the groups that face away from the camera. {@code groupQuads[f]} receives
	 * the quad count of group {@code f}. A quad is grouped by the side its winding faces, which is its normal's side
	 * except for back faces.
	 */
	public static boolean transcodeGrouped(ByteBuffer buf, int vertexCount, int slot, int[] groupQuads) {
		if (slot < 0 || slot > QuillCompactVertex.MAX_SLOT) throw new IllegalArgumentException("slot " + slot);
		if (vertexCount <= 0 || vertexCount % 4 != 0) return false;
		int base = buf.position();
		if ((long) vertexCount * SRC_STRIDE > buf.remaining()) return false;
		int quads = vertexCount / 4;
		byte[] faces = new byte[quads];
		int[] counts = new int[6];
		for (int q = 0; q < quads; q++) {
			int q0 = base + q * 4 * SRC_STRIDE;
			int face = validateQuad(buf, q0);
			if (face < 0) return false;
			// Normals stay as built (lighting reads them); the group follows the side the quad is actually visible from,
			// which differs for back faces such as the underside of a lava surface.
			int group = windingMatches(buf, q0, face) ? face : face ^ 1;
			faces[q] = (byte) (group << 3 | face);
			counts[group]++;
		}
		// Reordering breaks the read-before-write order the in-place path relies on, so read from a copy.
		byte[] copy = new byte[vertexCount * SRC_STRIDE];
		buf.get(base, copy);
		ByteBuffer src = ByteBuffer.wrap(copy).order(buf.order());
		int[] next = new int[6];
		for (int f = 1; f < 6; f++) next[f] = next[f - 1] + counts[f - 1];
		for (int q = 0; q < quads; q++) {
			int out = next[faces[q] >> 3]++;
			int f = faces[q] & 7;
			for (int k = 0; k < 4; k++) {
				writeVertex(src, (q * 4 + k) * SRC_STRIDE, buf, base + (out * 4 + k) * QuillCompactVertex.BYTES, f, slot);
			}
		}
		System.arraycopy(counts, 0, groupQuads, 0, 6);
		return true;
	}

	/**
	 * The quad's front side (counter-clockwise winding) points the same way as its face normal, using the signed area
	 * of all four corners so a degenerate first triangle doesn't decide it. Zero-area quads draw nothing and pass.
	 */
	static boolean windingMatches(ByteBuffer buf, int q0, int face) {
		int axis = face / 2;
		int a = SRC_POS + (axis + 1) % 3 * 4, b = SRC_POS + (axis + 2) % 3 * 4;
		float area = 0;
		for (int k = 0; k < 4; k++) {
			int p = q0 + k * SRC_STRIDE, n = q0 + (k + 1 & 3) * SRC_STRIDE;
			area += buf.getFloat(p + a) * buf.getFloat(n + b) - buf.getFloat(n + a) * buf.getFloat(p + b);
		}
		return face % 2 == 0 ? area >= 0 : area <= 0;
	}

	private static void writeVertex(ByteBuffer from, int src, ByteBuffer to, int dst, int face, int slot) {
		int qx = QuillCompactVertex.quantizePos(from.getFloat(src + SRC_POS));
		int qy = QuillCompactVertex.quantizePos(from.getFloat(src + SRC_POS + 4));
		int qz = QuillCompactVertex.quantizePos(from.getFloat(src + SRC_POS + 8));
		int color = from.getInt(src + SRC_COLOR);
		int qu = QuillCompactVertex.quantizeUv(from.getFloat(src + SRC_UV0));
		int qv = QuillCompactVertex.quantizeUv(from.getFloat(src + SRC_UV0 + 4));
		short lu = from.getShort(src + SRC_UV2);
		short lv = from.getShort(src + SRC_UV2 + 2);
		QuillCompactVertex.put(to, dst, qx, qy, qz, QuillCompactVertex.packW(face, slot), color, qu, qv, lu, lv);
	}

	/** Face index 0..5 for an axis-aligned, in-range quad starting at absolute index {@code q0}; -1 otherwise. */
	static int validateQuad(ByteBuffer buf, int q0) {
		int face = faceFromNormal(buf.get(q0 + SRC_NORMAL), buf.get(q0 + SRC_NORMAL + 1), buf.get(q0 + SRC_NORMAL + 2));
		if (face < 0) return -1;
		int axis = face / 2;
		float plane = buf.getFloat(q0 + SRC_POS + axis * 4);
		for (int k = 0; k < 4; k++) {
			int v = q0 + k * SRC_STRIDE;
			for (int c = 0; c < 3; c++) {
				if (QuillCompactVertex.quantizePos(buf.getFloat(v + SRC_POS + c * 4)) == Integer.MIN_VALUE) return -1;
			}
			if (!(Math.abs(buf.getFloat(v + SRC_POS + axis * 4) - plane) <= PLANE_EPS)) return -1;
			if (QuillCompactVertex.quantizeUv(buf.getFloat(v + SRC_UV0)) < 0) return -1;
			if (QuillCompactVertex.quantizeUv(buf.getFloat(v + SRC_UV0 + 4)) < 0) return -1;
		}
		return face;
	}

	/** Maps an exact unit-axis normal (signed bytes, ±127) to 0..5 = +X,-X,+Y,-Y,+Z,-Z; -1 for anything else. */
	static int faceFromNormal(byte nx, byte ny, byte nz) {
		if (ny == 0 && nz == 0 && (nx == 127 || nx == -127)) return nx > 0 ? 0 : 1;
		if (nx == 0 && nz == 0 && (ny == 127 || ny == -127)) return ny > 0 ? 2 : 3;
		if (nx == 0 && ny == 0 && (nz == 127 || nz == -127)) return nz > 0 ? 4 : 5;
		return -1;
	}
}
