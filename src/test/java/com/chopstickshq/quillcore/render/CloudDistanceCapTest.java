package com.chopstickshq.quillcore.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudDistanceCapTest {
	@Test
	void disabledPassesThrough() {
		assertEquals(64, CloudDistanceCap.applyChunkCap(false, true, true, 16, 64));
		assertEquals(64, CloudDistanceCap.applyChunkCap(true, false, true, 16, 64));
		assertEquals(64, CloudDistanceCap.applyChunkCap(true, true, false, 16, 64));
	}

	@Test
	void enabledCaps() {
		assertEquals(16, CloudDistanceCap.applyChunkCap(true, true, true, 16, 64));
		assertEquals(8, CloudDistanceCap.applyChunkCap(true, true, true, 16, 8));
	}
}
