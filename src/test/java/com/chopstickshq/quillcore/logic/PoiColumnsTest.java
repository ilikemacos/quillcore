package com.chopstickshq.quillcore.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoiColumnsTest {
	@Test
	void unknownUntilColumnLoaded() {
		PoiColumns c = new PoiColumns();
		assertEquals(PoiColumns.UNKNOWN, c.mask(5));
		c.section(5, 3);
		assertEquals(PoiColumns.UNKNOWN, c.mask(5));
	}

	@Test
	void sectionsAddBits() {
		PoiColumns c = new PoiColumns();
		c.column(7, 0);
		c.section(7, 0);
		c.section(7, 23);
		assertEquals(1L | 1L << 23, c.mask(7));
	}

	@Test
	void reloadReplacesMask() {
		PoiColumns c = new PoiColumns();
		c.column(1, 0b1010);
		c.column(1, 0b1);
		assertEquals(1, c.mask(1));
	}
}
