package com.chopstickshq.quillcore.upscale;

/**
 * Modular upscaling backend. Unavailable backends must not prevent QuillCore from loading.
 * Only claim a vendor algorithm when a real path exists (never bilinear labelled as FSR).
 */
public interface UpscaleBackend {
	String id();
	String displayName();
	boolean isAvailable();
	String unavailableReason();
}
