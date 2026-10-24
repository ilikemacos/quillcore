package com.chopstickshq.quillcore.render.region;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RegionArenaTest {
	@Test
	void allocsAreAlignedAndApart() {
		RegionArena a = new RegionArena(1024, 16);
		int x = a.alloc(10), y = a.alloc(20);
		assertEquals(0, x);
		assertEquals(16, y);
		assertEquals(48, a.used());
	}

	@Test
	void freeMergesBothSides() {
		RegionArena a = new RegionArena(64, 16);
		int x = a.alloc(16), y = a.alloc(16), z = a.alloc(16);
		a.free(x, 16);
		a.free(z, 16);
		assertEquals(2, a.holes());
		a.free(y, 16);
		assertEquals(1, a.holes());
		assertEquals(0, a.used());
	}

	@Test
	void growsWhenFull() {
		RegionArena a = new RegionArena(32, 16);
		a.alloc(32);
		assertEquals(32, a.alloc(40));
		assertEquals(128, a.capacity());
		assertEquals(1, a.holes());
	}

	@Test
	void doubleFreeThrows() {
		RegionArena a = new RegionArena(64, 16);
		int x = a.alloc(16);
		a.free(x, 16);
		assertThrows(IllegalStateException.class, () -> a.free(x, 16));
	}

	@Test
	void randomChurnNeverOverlaps() {
		RegionArena a = new RegionArena(256, 4);
		Random r = new Random(7);
		List<int[]> live = new ArrayList<>();
		for (int i = 0; i < 5000; i++) {
			if (!live.isEmpty() && r.nextInt(3) == 0) {
				int[] g = live.remove(r.nextInt(live.size()));
				a.free(g[0], g[1]);
			} else {
				int size = 1 + r.nextInt(300);
				int off = a.alloc(size);
				int end = off + (size + 3) / 4 * 4;
				assertTrue(end <= a.capacity());
				for (int[] g : live) assertTrue(end <= g[0] || off >= g[0] + (g[1] + 3) / 4 * 4, "overlap");
				live.add(new int[]{off, size});
			}
		}
		for (int[] g : live) a.free(g[0], g[1]);
		assertEquals(0, a.used());
		assertEquals(1, a.holes());
	}
}
