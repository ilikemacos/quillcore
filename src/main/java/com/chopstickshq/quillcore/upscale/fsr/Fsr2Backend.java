package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/**
 * Stub — FSR2 is temporal and needs color, depth, motion vectors, and jitter.
 * Minecraft 1.21.8 / Sodium do not expose a usable MV buffer for this mod.
 */
public final class Fsr2Backend implements UpscaleBackend {
	@Override
	public String id() {
		return "fsr2";
	}

	@Override
	public String displayName() {
		return "AMD FSR2 (temporal — not available)";
	}

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public String unavailableReason() {
		return "FSR2 requires per-frame motion vectors, depth, and projection jitter; "
				+ "MC 1.21.8 Fabric/Sodium do not provide a QuillCore-accessible MV pass. "
				+ "Interface reserved for a future path. See docs/FEASIBILITY.md.";
	}
}
