package com.chopstickshq.quillcore.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class IntArrayInternerTest {
	@Test
	void equalArraysShareOneInstance() {
		IntArrayInterner pool = new IntArrayInterner();
		int[] a = {1, 2, 3};
		assertSame(a, pool.intern(a));
		assertSame(a, pool.intern(new int[]{1, 2, 3}));
		assertEquals(1, pool.size());
	}

	@Test
	void differentArraysStayApart() {
		IntArrayInterner pool = new IntArrayInterner();
		int[] a = pool.intern(new int[]{1, 2, 3});
		int[] b = pool.intern(new int[]{1, 2, 4});
		assertNotSame(a, b);
		assertEquals(2, pool.size());
	}

	@Test
	void clearForgetsEverything() {
		IntArrayInterner pool = new IntArrayInterner();
		int[] a = pool.intern(new int[]{7});
		pool.clear();
		int[] b = new int[]{7};
		assertSame(b, pool.intern(b));
		assertNotSame(a, b);
	}
}
