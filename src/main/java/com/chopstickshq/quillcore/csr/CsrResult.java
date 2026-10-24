package com.chopstickshq.quillcore.csr;

import java.util.Optional;

/** Value-or-reason. Loaders return this instead of throwing. */
public record CsrResult<T>(T value, String error) {
	public static <T> CsrResult<T> ok(T value) {
		return new CsrResult<>(value, null);
	}

	public static <T> CsrResult<T> fail(String error) {
		return new CsrResult<>(null, error == null ? "unknown error" : error);
	}

	public boolean isOk() {
		return value != null;
	}

	public Optional<T> asOptional() {
		return Optional.ofNullable(value);
	}
}
