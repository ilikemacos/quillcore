package com.chopstickshq.quillcore.upscale;

/** An upscaler. A backend that isn't available must not stop the mod from loading. */
public interface UpscaleBackend {
	String id();
	String displayName();
	boolean isAvailable();
	String unavailableReason();
}
