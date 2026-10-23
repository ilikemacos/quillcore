package com.chopstickshq.quillcore.upscale.framefx;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/**
 * Stub adapter for Universal-FrameFX (https://github.com/ilikemacos/universal-framefx).
 * FrameFX is a Windows capture+overlay app (MIT app / closed image engine).
 * There is no in-process Minecraft OpenGL plugin API in 0.1.0 — NOT IMPLEMENTED.
 * CSR is explicitly excluded from QuillCore and is not an integration path here.
 */
public final class FrameFxBackend implements UpscaleBackend {
	@Override public String id() { return "framefx"; }
	@Override public String displayName() { return "FrameFX adapter (not implemented)"; }
	@Override public boolean isAvailable() { return false; }
	@Override public String unavailableReason() {
		return "FrameFX runs as an external Windows overlay; no valid in-process MC integration in 0.1.0. See docs/FEASIBILITY.md.";
	}
}
