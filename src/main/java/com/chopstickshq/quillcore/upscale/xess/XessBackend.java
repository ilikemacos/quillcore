package com.chopstickshq.quillcore.upscale.xess;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/** Stub — not XeSS. See docs/FEASIBILITY.md. */
public final class XessBackend implements UpscaleBackend {
	@Override public String id() { return "xess"; }
	@Override public String displayName() { return "Intel XeSS (not implemented)"; }
	@Override public boolean isAvailable() { return false; }
	@Override public String unavailableReason() {
		return "No XeSS SDK integration in QuillCore 0.1.0. Official paths target DX12/Vulkan; MC Java defaults to OpenGL.";
	}
}
