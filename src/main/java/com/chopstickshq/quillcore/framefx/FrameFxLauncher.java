package com.chopstickshq.quillcore.framefx;

import com.chopstickshq.quillcore.common.QuillCore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Finds and starts Universal-FrameFX (https://github.com/ilikemacos/universal-framefx)
 * as a separate process. It never touches the game's renderer.
 */
public final class FrameFxLauncher {
	private static final String[] CANDIDATE_NAMES = {
			"Universal-FrameFX.exe",
			"UniversalFrameFX.exe",
			"FrameFX.exe"
	};

	private FrameFxLauncher() {}

	public static boolean isWindows() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return os.contains("win");
	}

	/** Configured path first, then PATH. */
	public static Optional<Path> resolveExecutable(String configuredPath) {
		if (configuredPath != null && !configuredPath.isBlank()) {
			Path p = Path.of(configuredPath.trim());
			if (Files.isRegularFile(p)) {
				return Optional.of(p.toAbsolutePath().normalize());
			}
		}
		if (!isWindows()) {
			return Optional.empty();
		}
		String pathEnv = System.getenv("PATH");
		if (pathEnv != null) {
			for (String dir : pathEnv.split(File.pathSeparator)) {
				if (dir.isBlank()) continue;
				for (String name : CANDIDATE_NAMES) {
					Path cand = Path.of(dir, name);
					if (Files.isRegularFile(cand)) {
						return Optional.of(cand.toAbsolutePath().normalize());
					}
				}
			}
		}
		return Optional.empty();
	}

	public static String probeSummary(String configuredPath) {
		if (!isWindows()) {
			return "FrameFX launcher: unavailable (Windows-only external app)";
		}
		Optional<Path> exe = resolveExecutable(configuredPath);
		return exe.map(path -> "FrameFX launcher: found " + path)
				.orElse("FrameFX launcher: exe not found (set frameFxExecutablePath in config)");
	}

	/**
	 * Starts FrameFX with no arguments; modes are picked in its own UI.
	 * Returns an error message, or empty on success.
	 */
	public static Optional<String> launch(String configuredPath) {
		if (!isWindows()) {
			return Optional.of("FrameFX is a Windows overlay app; this OS cannot launch it.");
		}
		Optional<Path> exe = resolveExecutable(configuredPath);
		if (exe.isEmpty()) {
			return Optional.of("Universal-FrameFX executable not found. Set frameFxExecutablePath or install to PATH.");
		}
		List<String> cmd = new ArrayList<>();
		cmd.add(exe.get().toString());
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(exe.get().getParent() != null ? exe.get().getParent().toFile() : null);
		try {
			pb.start();
			QuillCore.LOGGER.info("Launched Universal-FrameFX: {}", exe.get());
			return Optional.empty();
		} catch (IOException e) {
			QuillCore.LOGGER.error("Failed to launch FrameFX at {}", exe.get(), e);
			return Optional.of("Launch failed: " + e.getMessage());
		}
	}
}
