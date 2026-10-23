package com.chopstickshq.quillcore.upscale.fsr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FsrQualityPresetTest {
	@Test
	void scalesMatchAmdRatios() {
		assertEquals(1.0f / 1.5f, FsrQualityPreset.QUALITY.renderScale(), 1e-5f);
		assertEquals(0.5f, FsrQualityPreset.PERFORMANCE.renderScale(), 1e-5f);
		assertEquals(1.0f / 3.0f, FsrQualityPreset.ULTRA_PERFORMANCE.renderScale(), 1e-5f);
	}

	@Test
	void fromIdParses() {
		assertEquals(FsrQualityPreset.BALANCED, FsrQualityPreset.fromId("balanced"));
		assertEquals(FsrQualityPreset.QUALITY, FsrQualityPreset.fromId("nope"));
		assertEquals(FsrQualityPreset.ULTRA_QUALITY, FsrQualityPreset.fromId("Ultra Quality"));
	}

	@Test
	void scaleDimensionEvenAndPositive() {
		assertEquals(1280, FsrQualityPreset.scaleDimension(1920, 2.0f / 3.0f));
		assertEquals(1, FsrQualityPreset.scaleDimension(1, 0.1f));
		assertTrue((FsrQualityPreset.scaleDimension(1921, 0.5f) & 1) == 0);
	}
}
