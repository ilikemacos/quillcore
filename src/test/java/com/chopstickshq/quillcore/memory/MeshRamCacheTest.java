package com.chopstickshq.quillcore.memory;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MeshRamCacheTest {
	static ByteBuffer mesh(int n) {
		return ByteBuffer.allocate(n);
	}

	@Test
	void evictsOldestToLowMark() {
		MeshRamCache c = new MeshRamCache(1000, 10_000, 10_000);
		for (long k = 0; k < 9; k++) assertEquals(0, c.uploaded(k, mesh(100)).length);
		c.drawn(0);
		assertArrayEquals(new long[] {1, 2}, c.uploaded(9, mesh(100)));
		assertEquals(800, c.gpuBytes());
		assertTrue(c.isEvicted(1));
	}

	@Test
	void neverEvictsWithoutRamCopy() {
		MeshRamCache c = new MeshRamCache(1000, 0, 10_000);
		for (long k = 0; k < 12; k++) assertEquals(0, c.uploaded(k, mesh(100)).length);
		assertEquals(1200, c.gpuBytes());
	}

	@Test
	void evictedCopiesSurviveRamPressure() {
		MeshRamCache c = new MeshRamCache(250, 300, 10_000);
		c.uploaded(1, mesh(100));
		c.uploaded(2, mesh(100));
		assertArrayEquals(new long[] {1}, c.uploaded(3, mesh(100)));
		c.uploaded(4, mesh(100));
		assertNotNull(c.restore(1));
		assertFalse(c.isEvicted(1));
	}

	@Test
	void restoreIsThrottled() {
		MeshRamCache c = new MeshRamCache(100, 10_000, 150);
		c.uploaded(1, mesh(100));
		c.uploaded(2, mesh(100));
		c.uploaded(3, mesh(100));
		assertTrue(c.isEvicted(1) && c.isEvicted(2));
		c.endFrame();
		assertNotNull(c.restore(1));
		assertNull(c.restore(2));
		c.endFrame();
		assertNotNull(c.restore(2));
	}

	@Test
	void freeForgetsEverything() {
		MeshRamCache c = new MeshRamCache(1000, 1000, 1000);
		c.uploaded(1, mesh(100));
		c.freed(1);
		assertEquals(0, c.gpuBytes());
		assertEquals(0, c.ramBytes());
	}
}
