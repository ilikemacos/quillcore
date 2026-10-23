package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.upscale.UpscaleBackend;

/** Old "fsr" id, same as fsr1. */
public final class FsrBackend implements UpscaleBackend {
	private final Fsr1Backend delegate = new Fsr1Backend();

	@Override
	public String id() {
		return "fsr";
	}

	@Override
	public String displayName() {
		return delegate.displayName() + " [legacy id]";
	}

	@Override
	public boolean isAvailable() {
		return delegate.isAvailable();
	}

	@Override
	public String unavailableReason() {
		return delegate.unavailableReason();
	}
}
