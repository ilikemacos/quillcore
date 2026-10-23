package com.chopstickshq.quillcore.upscale;

import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.upscale.fsr.Fsr1Backend;
import com.chopstickshq.quillcore.upscale.fsr.FsrBackend;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UpscaleRegistry {
	private static final Map<String, UpscaleBackend> BACKENDS = new LinkedHashMap<>();

	private UpscaleRegistry() {}

	public static void bootstrap() {
		BACKENDS.clear();
		register(new Fsr1Backend());
		register(new FsrBackend()); // old id for fsr1
		QuillCore.LOGGER.info("Upscale backends: {}", summarize());
	}

	private static void register(UpscaleBackend backend) {
		BACKENDS.put(backend.id(), backend);
	}

	public static Map<String, UpscaleBackend> all() {
		return Collections.unmodifiableMap(BACKENDS);
	}

	public static UpscaleBackend get(String id) {
		if (id == null || id.isBlank() || "none".equalsIgnoreCase(id)) {
			return null;
		}
		return BACKENDS.get(id.trim().toLowerCase());
	}

	public static String summarize() {
		StringBuilder sb = new StringBuilder();
		for (UpscaleBackend b : BACKENDS.values()) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(b.id()).append('=').append(b.isAvailable() ? "available" : "unavailable");
		}
		return sb.toString();
	}
}
