package com.chopstickshq.quillcore.csr;

import java.util.function.Consumer;

/**
 * Session state of one CSR feature. Any failure disables it for the rest of the
 * session and warns exactly once through the injected sink; the game keeps rendering vanilla.
 */
public final class CsrFeature {
	private final String name;
	private final Consumer<String> warn;
	private boolean dead;
	private String reason = "";

	public CsrFeature(String name, Consumer<String> warn) {
		this.name = name;
		this.warn = warn;
	}

	public String name() {
		return name;
	}

	public boolean isDisabled() {
		return dead;
	}

	public String reason() {
		return reason;
	}

	/** Disable for the session; warns only on the first call. */
	public void disable(String why) {
		if (dead) {
			return;
		}
		dead = true;
		reason = why == null ? "unknown" : why;
		try {
			warn.accept(name + " disabled for this session: " + reason + ". Rendering continues without it.");
		} catch (RuntimeException ignored) {
			// A broken log sink must never take the game down.
		}
	}

	public void disable(String where, Throwable t) {
		disable(where + ": " + t);
	}

	/**
	 * Gate on the config flag and loaded weights. Returns the weights when the feature may run,
	 * otherwise {@code null} (disabling it if the weights are bad).
	 */
	public <T> T tryEnable(boolean requested, CsrResult<T> weights) {
		if (!requested || dead) {
			return null;
		}
		if (weights == null || !weights.isOk()) {
			disable(weights == null ? "weights not loaded" : weights.error());
			return null;
		}
		return weights.value();
	}
}
