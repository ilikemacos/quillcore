package com.chopstickshq.quillcore.render.mesh;

/**
 * Vanilla 1.21.8 block vertex size, used as the baseline for footprint comparisons.
 *
 * <p>Source: {@code javap -c com.mojang.blaze3d.vertex.DefaultVertexFormat} / {@code VertexFormatElement} on the
 * local Loom jar {@code minecraft-clientonly-1.21.8-loom.mappings.1_21_8...jar}. {@code DefaultVertexFormat.BLOCK} is
 * {@code Position (FLOAT×3) + Color (UBYTE×4) + UV0 (FLOAT×2) + UV2 (SHORT×2) + Normal (BYTE×3) + padding(1)}
 * = 12 + 4 + 8 + 4 + 3 + 1 = 32 bytes. There is no overlay (UV1) element in BLOCK.
 */
public final class VanillaVertexFootprint {
	public static final int BLOCK_BYTES_PER_VERTEX = 32;

	private VanillaVertexFootprint() {}

	public static long bytesFor(long vertexCount) {
		return vertexCount * BLOCK_BYTES_PER_VERTEX;
	}
}
