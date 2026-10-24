package com.chopstickshq.quillcore.render.mesh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class FaceRunsTest {
	@Test
	void allVisibleIsOneRun() {
		assertArrayEquals(new int[] {0, 21}, FaceRuns.runs(new int[] {1, 2, 3, 4, 5, 6}, 0x3F));
	}

	@Test
	void hiddenGroupSplitsRuns() {
		// hide -X (2 quads) and +Z (5 quads)
		assertArrayEquals(new int[] {0, 1, 3, 7, 15, 6}, FaceRuns.runs(new int[] {1, 2, 3, 4, 5, 6}, 0b101101));
	}

	@Test
	void emptyHiddenGroupDoesNotSplit() {
		assertArrayEquals(new int[] {0, 3}, FaceRuns.runs(new int[] {1, 0, 2, 0, 0, 0}, 0b111101));
	}

	@Test
	void nothingVisible() {
		assertArrayEquals(new int[0], FaceRuns.runs(new int[] {1, 2, 0, 0, 0, 0}, 0));
	}
}
