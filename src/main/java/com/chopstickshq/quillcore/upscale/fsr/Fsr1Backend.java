package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/**
 * AMD FidelityFX Super Resolution 1 (EASU + RCAS) — spatial upscale.
 * MIT (GPUOpen). See {@code third_party/amd-fsr1/NOTICE}.
 * Runtime path: shrink main RT → world → EASU/RCAS → restore main → GUI.
 */
public final class Fsr1Backend implements UpscaleBackend {
	@Override
	public String id() {
		return "fsr1";
	}

	@Override
	public String displayName() {
		return "AMD FSR1 (EASU+RCAS spatial)";
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public String unavailableReason() {
		return "";
	}
}
