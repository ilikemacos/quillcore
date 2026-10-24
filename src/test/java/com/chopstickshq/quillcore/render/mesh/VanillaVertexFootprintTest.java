package com.chopstickshq.quillcore.render.mesh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaVertexFootprintTest {
	@Test
	void constantMatchesHandComputedBlockFormat() {
		// pos 3×f32 + color 4×u8 + uv0 2×f32 + uv2 2×s16 + normal 3×s8 + 1 pad
		assertEquals(12 + 4 + 8 + 4 + 3 + 1, VanillaVertexFootprint.BLOCK_BYTES_PER_VERTEX);
		assertEquals(6144L * 32, VanillaVertexFootprint.bytesFor(6144));
	}
}
