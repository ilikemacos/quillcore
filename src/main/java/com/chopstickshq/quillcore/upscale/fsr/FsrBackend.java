package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/** FSR1 placeholder. Needs a resizable main render target before it can work. */
public final class FsrBackend implements UpscaleBackend {
	@Override public String id() { return "fsr"; }
	@Override public String displayName() { return "AMD FSR1"; }
	@Override public boolean isAvailable() { return false; }
	@Override public String unavailableReason() {
		return "not implemented yet";
	}
}
