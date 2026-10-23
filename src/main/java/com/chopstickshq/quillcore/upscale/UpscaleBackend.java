package com.chopstickshq.quillcore.upscale;

/**
 * Modular upscaling backend. Unavailable backends must not prevent QuillCore from loading.
 * Implementations in 0.1.0 are stubs — do not claim FSR/XeSS/FrameFX without a real path.
 */
public interface UpscaleBackend {
	String id();
	String displayName();
	boolean isAvailable();
	String unavailableReason();
}
