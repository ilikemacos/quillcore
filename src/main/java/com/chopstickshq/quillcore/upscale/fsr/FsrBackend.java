package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/** Stub, not FSR. */
public final class FsrBackend implements UpscaleBackend {
	@Override public String id() { return "fsr"; }
	@Override public String displayName() { return "AMD FSR (not implemented)"; }
	@Override public boolean isAvailable() { return false; }
	@Override public String unavailableReason() {
		return "No FSR SDK integration in QuillCore 0.1.0. Temporal FSR needs motion vectors/depth in the MC pipeline.";
	}
}
