package com.chopstickshq.quillcore.compat;

import com.chopstickshq.quillcore.framegen.NoopFrameGenBackend;
import com.chopstickshq.quillcore.upscale.UpscaleBackend;
import com.chopstickshq.quillcore.upscale.UpscaleRegistry;

public final class CompatStatus {
	private CompatStatus() {}

	public static String summarize() {
		StringBuilder sb = new StringBuilder("backends: ");
		boolean first = true;
		for (UpscaleBackend b : UpscaleRegistry.all().values()) {
			if (!first) sb.append("; ");
			first = false;
			sb.append(b.id()).append(b.isAvailable() ? " OK" : " unavailable (" + b.unavailableReason() + ")");
		}
		NoopFrameGenBackend fg = new NoopFrameGenBackend();
		sb.append("; framegen=").append(fg.id()).append(" unavailable (").append(fg.unavailableReason()).append(')');
		return sb.toString();
	}
}
