package com.chopstickshq.quillcore.framegen;

public final class NoopFrameGenBackend implements FrameGenBackend {
	@Override public String id() { return "none"; }
	@Override public boolean isAvailable() { return false; }
	@Override public String unavailableReason() {
		return "Frame generation needs motion vectors and a presentation path not wired in 0.1.0.";
	}
}
