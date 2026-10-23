package com.chopstickshq.quillcore.upscale.framefx;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/**
 * In-process upscale stub for Universal-FrameFX.
 * FrameFX is a Windows capture+overlay app — there is no in-process MC GL/Vulkan plugin API.
 * External launch lives in {@code com.chopstickshq.quillcore.framefx.FrameFxLauncher}
 * and is deliberately NOT registered here. CSR is excluded.
 */
public final class FrameFxBackend implements UpscaleBackend {
	@Override public String id() { return "framefx"; }
	@Override public String displayName() { return "FrameFX in-process (unavailable)"; }
	@Override public boolean isAvailable() { return false; }
	@Override public String unavailableReason() {
		return "FrameFX is external-only. Use /quillcore framefx launch (Windows). "
				+ "This UpscaleBackend stays unavailable so QuillCore never claims in-process compositing. See docs/FEASIBILITY.md.";
	}
}
