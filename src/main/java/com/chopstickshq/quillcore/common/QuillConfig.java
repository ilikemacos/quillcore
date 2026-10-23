package com.chopstickshq.quillcore.common;

import com.chopstickshq.quillcore.logic.IdleAiThrottle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent QuillCore settings at {@code config/quillcore.json}.
 * Every toggle here is wired to real behavior (or an honest "unavailable" backend status).
 */
public final class QuillConfig {
	public static final int CURRENT_VERSION = 1;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static QuillConfig INSTANCE = new QuillConfig();

	public int configVersion = CURRENT_VERSION;
	public boolean masterEnabled = true;
	public boolean renderEnabled = true;
	public boolean logicEnabled = true;
	public boolean idleAiThrottleEnabled = true;
	public int idleAiThrottleMaxSkipTicks = 2;
	public double idleAiThrottlePlayerRadiusBlocks = 32.0;
	public boolean benchEnabled = false;

	private QuillConfig() {}

	public static QuillConfig get() {
		return INSTANCE;
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("quillcore.json");
	}

	public static void load() {
		Path file = path();
		if (!Files.isRegularFile(file)) {
			INSTANCE = new QuillConfig();
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			QuillConfig loaded = GSON.fromJson(reader, QuillConfig.class);
			if (loaded == null) {
				INSTANCE = new QuillConfig();
			} else {
				loaded.sanitize();
				INSTANCE = loaded;
			}
		} catch (Exception e) {
			QuillCore.LOGGER.error("Failed to load {}; using defaults", file, e);
			INSTANCE = new QuillConfig();
		}
		save();
	}

	public static void save() {
		INSTANCE.sanitize();
		Path file = path();
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (IOException e) {
			QuillCore.LOGGER.error("Failed to save {}", file, e);
		}
	}

	public void sanitize() {
		configVersion = CURRENT_VERSION;
		idleAiThrottleMaxSkipTicks = IdleAiThrottle.clamp(idleAiThrottleMaxSkipTicks, 0, 10);
		if (Double.isNaN(idleAiThrottlePlayerRadiusBlocks) || idleAiThrottlePlayerRadiusBlocks < 0) {
			idleAiThrottlePlayerRadiusBlocks = 32.0;
		}
	}

	public String statusSummary() {
		return "master=" + masterEnabled
				+ " render=" + renderEnabled
				+ " logic=" + logicEnabled
				+ " idleAiThrottle=" + idleAiThrottleEnabled
				+ " maxSkip=" + idleAiThrottleMaxSkipTicks
				+ " radius=" + idleAiThrottlePlayerRadiusBlocks
				+ " bench=" + benchEnabled;
	}
}
