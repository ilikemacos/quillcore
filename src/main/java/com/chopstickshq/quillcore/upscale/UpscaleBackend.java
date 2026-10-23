package com.chopstickshq.quillcore.upscale;

public interface UpscaleBackend {
	String id();
	String displayName();
	boolean isAvailable();
	String unavailableReason();
}
