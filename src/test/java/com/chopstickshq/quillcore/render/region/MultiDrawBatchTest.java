package com.chopstickshq.quillcore.render.region;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MultiDrawBatchTest {
	@Test
	void mergesTouchingRanges() {
		MultiDrawBatch b = new MultiDrawBatch();
		b.add(6, 0, 0, 4);
		b.add(12, 24, 0, 4);
		b.add(6, 200, 0, 4);
		b.add(6, 224, 64, 4);
		assertEquals(3, b.size());
		assertEquals(18, b.count(0));
		assertEquals(64, b.base(2));
	}

	@Test
	void growsAndClears() {
		MultiDrawBatch b = new MultiDrawBatch();
		for (int i = 0; i < 100; i++) b.add(6, i * 1000L, i, 4);
		assertEquals(100, b.size());
		b.clear();
		b.add(0, 0, 0, 4);
		assertEquals(0, b.size());
	}
}
