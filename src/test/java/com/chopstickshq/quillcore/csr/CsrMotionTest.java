package com.chopstickshq.quillcore.csr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CsrMotionTest {
	@Test
	void pyramidAndTileSizes() {
		int w = 1921;
		int[] expect = {1921, 960, 480, 240};
		for (int l = 0; l < CsrMotion.LEVELS; l++) {
			assertEquals(expect[l], w);
			w = CsrMotion.halve(w);
		}
		assertEquals(1, CsrMotion.halve(1));
		assertEquals(241, CsrMotion.tiles(1921));
		assertEquals(135, CsrMotion.tiles(1080));
		assertEquals(31, CsrMotion.blocks(241));
	}

	@Test
	void evenHalveIsTheTwoByTwoMean() {
		float[] src = {
				0, 2, 4, 6,
				8, 10, 12, 14,
		};
		assertArrayEquals(new float[] {5, 9}, CsrMotion.halve(src, 4, 2), 1e-6f);
	}

	@Test
	void oddHalveSamplesAtTheDestinationCentre() {
		// 3 -> 1: the centre of the only output texel lands on source texel 1 exactly.
		assertArrayEquals(new float[] {7}, CsrMotion.halve(new float[] {1, 7, 100}, 3, 1), 1e-6f);
		// 5 -> 2: centres at 0.75 and 3.25 in texel space.
		float[] out = CsrMotion.halve(new float[] {0, 4, 8, 12, 16}, 5, 1);
		assertArrayEquals(new float[] {3, 13}, out, 1e-5f);
	}

	@Test
	void constantImageStaysConstant() {
		float[] src = new float[7 * 5];
		java.util.Arrays.fill(src, 0.42f);
		for (float v : CsrMotion.halve(src, 7, 5)) {
			assertEquals(0.42f, v, 1e-6f);
		}
	}
}
