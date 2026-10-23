package com.chopstickshq.quillcore.framefx;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameFxLauncherTest {
	@Test
	void probeSummaryNeverClaimsInProcess() {
		String s = FrameFxLauncher.probeSummary("");
		assertFalse(s.toLowerCase().contains("in-process"));
		assertTrue(s.contains("FrameFX launcher"));
	}

	@Test
	void missingConfiguredPathResolvesEmpty() {
		Optional<?> r = FrameFxLauncher.resolveExecutable("/definitely/not/a/real/framefx-binary-quillcore-test");
		assertTrue(r.isEmpty());
	}

	@Test
	void launchOnNonWindowsReportsErrorWhenNotWindows() {
		if (FrameFxLauncher.isWindows()) {
			return; // skip assertion on Windows CI
		}
		Optional<String> err = FrameFxLauncher.launch("");
		assertTrue(err.isPresent());
		assertTrue(err.get().toLowerCase().contains("windows"));
	}
}
