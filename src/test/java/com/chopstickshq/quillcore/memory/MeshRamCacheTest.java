package com.chopstickshq.quillcore.memory;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MeshRamCacheTest {
	@Test
	void evictsOldestToLowMark() {
		MeshRamCache c = new MeshRamCache(1000, 10_000, 10_000);
		for (long k = 0; k < 9; k++) assertEquals(0, c.uploaded(k, 100).length);
		c.drawn(0);
		long[] out = c.uploaded(9, 100);
		assertArrayEquals(new long[] {1, 2}, out);
		assertEquals(800, c.gpuBytes());
	}

	@Test
	void parkedMeshComesBackOncePerQuota() {
		MeshRamCache c = new MeshRamCache(10_000, 10_000, 150);
		c.park(1, ByteBuffer.allocate(100));
		c.park(2, ByteBuffer.allocate(100));
		assertNotNull(c.take(1));
		assertNull(c.take(2));
		c.endFrame();
		assertNotNull(c.take(2));
		assertEquals(0, c.ramBytes());
	}

	@Test
	void ramPoolIsLruCapped() {
		MeshRamCache c = new MeshRamCache(10_000, 250, 10_000);
		c.park(1, ByteBuffer.allocate(100));
		c.park(2, ByteBuffer.allocate(100));
		c.park(3, ByteBuffer.allocate(100));
		assertFalse(c.parked(1));
		assertTrue(c.parked(3));
		assertEquals(200, c.ramBytes());
	}

	@Test
	void offWhenNoRam() {
		MeshRamCache c = new MeshRamCache(10_000, 0, 10_000);
		c.park(1, ByteBuffer.allocate(10));
		assertNull(c.take(1));
	}
}
