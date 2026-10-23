package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/**
 * Stub — FSR3 upscale needs FSR2 inputs; frame generation needs pacing + HUD-safe FG.
 * Frame generation stays disabled regardless of config.
 */
public final class Fsr3Backend implements UpscaleBackend {
	@Override
	public String id() {
		return "fsr3";
	}

	@Override
	public String displayName() {
		return "AMD FSR3 (upscale+FG — not available)";
	}

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public String unavailableReason() {
		return "FSR3 upscale needs FSR2 motion vectors/depth/jitter (unavailable). "
				+ "FSR3 frame generation is also disabled — no HUD-safe frame-pacing hook. "
				+ "See docs/FEASIBILITY.md.";
	}
}
