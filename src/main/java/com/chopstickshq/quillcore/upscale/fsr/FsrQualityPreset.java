package com.chopstickshq.quillcore.upscale.fsr;

/**
 * AMD FSR1 quality modes → internal render scale (source / display).
 * Scales match AMD's published FSR1 ratios (display multiplier inverted).
 */
public enum FsrQualityPreset {
	/** 1.3× upscale → ~0.770 render scale. */
	ULTRA_QUALITY(1.0f / 1.3f, "Ultra Quality"),
	/** 1.5× → ~0.667. */
	QUALITY(1.0f / 1.5f, "Quality"),
	/** 1.7× → ~0.588. */
	BALANCED(1.0f / 1.7f, "Balanced"),
	/** 2.0× → 0.5. */
	PERFORMANCE(0.5f, "Performance"),
	/** 3.0× → ~0.333 (FSR2 Ultra Performance naming; valid spatial scale). */
	ULTRA_PERFORMANCE(1.0f / 3.0f, "Ultra Performance");

	private final float renderScale;
	private final String label;

	FsrQualityPreset(float renderScale, String label) {
		this.renderScale = renderScale;
		this.label = label;
	}

	public float renderScale() {
		return renderScale;
	}

	public String label() {
		return label;
	}

	public static FsrQualityPreset fromId(String id) {
		if (id == null || id.isBlank()) {
			return QUALITY;
		}
		String key = id.trim().toUpperCase().replace('-', '_').replace(' ', '_');
		try {
			return FsrQualityPreset.valueOf(key);
		} catch (IllegalArgumentException e) {
			return QUALITY;
		}
	}

	/** Scaled dimension, even, at least 1. */
	public static int scaleDimension(int nativeSize, float scale) {
		int scaled = Math.max(1, Math.round(nativeSize * scale));
		if ((scaled & 1) != 0) {
			scaled = Math.max(1, scaled - 1);
		}
		return scaled;
	}
}
