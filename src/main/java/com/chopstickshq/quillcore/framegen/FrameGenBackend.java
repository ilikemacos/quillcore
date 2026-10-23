package com.chopstickshq.quillcore.framegen;

/** Optional frame-generation backend. Stub only in 0.1.0. */
public interface FrameGenBackend {
	String id();
	boolean isAvailable();
	String unavailableReason();
}
