package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/** AMD FSR1 (EASU + RCAS). MIT, see third_party/amd-fsr1. */
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
