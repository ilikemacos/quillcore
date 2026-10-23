package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/**
 * Stub, spatial FSR1 not wired. Temporal FSR2/3 also unavailable.
 * See docs/FEASIBILITY.md §1 for the exact missing hook on Fabric 1.21.8.
 */
public final class FsrBackend implements UpscaleBackend {
	@Override public String id() { return "fsr"; }
	@Override public String displayName() { return "AMD FSR1 spatial (not implemented)"; }
	@Override public boolean isAvailable() { return false; }
	@Override public String unavailableReason() {
		return "No Sodium-safe post-composite color-buffer hook on Fabric 1.21.8 "
				+ "(need lower-res main RT before renderLevel + EASU/RCAS before GUI; "
				+ "WorldRenderEvents ends before hand/GUI but does not expose a resizable color FBO; "
				+ "blit path is GpuDevice.presentTexture).";
	}
}
