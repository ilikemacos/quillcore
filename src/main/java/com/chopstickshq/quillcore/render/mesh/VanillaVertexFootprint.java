package com.chopstickshq.quillcore.render.mesh;

/**
 * Vanilla 1.21.8 block vertex size, used as the baseline for footprint comparisons.
 *
 * <p>{@code DefaultVertexFormat.BLOCK}: position 12, color 4, uv0 8, uv2 4, normal 3, padding 1.
 */
public final class VanillaVertexFootprint {
	public static final int BLOCK_BYTES_PER_VERTEX = 32;

	private VanillaVertexFootprint() {}

	public static long bytesFor(long vertexCount) {
		return vertexCount * BLOCK_BYTES_PER_VERTEX;
	}
}
