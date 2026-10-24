package com.chopstickshq.quillcore.render.region;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegionPosTest {
	@Test
	void roundTripsNegatives() {
		long r = RegionPos.of(-1, -5, -9);
		assertEquals(-1, RegionPos.x(r));
		assertEquals(-2, RegionPos.y(r));
		assertEquals(-2, RegionPos.z(r));
	}

	@Test
	void slotsCoverRegionOnce() {
		Set<Integer> seen = new HashSet<>();
		for (int x = 8; x < 16; x++) for (int y = -4; y < 0; y++) for (int z = 0; z < 8; z++) {
			assertEquals(RegionPos.of(8, -4, 0), RegionPos.of(x, y, z));
			assertTrue(seen.add(RegionPos.slot(x, y, z)));
		}
		assertEquals(256, seen.size());
	}
}
