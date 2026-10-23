package com.chopstickshq.quillcore.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherRadiusCapTest {
	@Test
	void disabledPassesThrough() {
		assertEquals(10, WeatherRadiusCap.applyRadius(true, true, false, 6, 10));
	}

	@Test
	void enabledCaps() {
		assertEquals(6, WeatherRadiusCap.applyRadius(true, true, true, 6, 10));
		assertEquals(5, WeatherRadiusCap.applyRadius(true, true, true, 6, 5));
	}
}
