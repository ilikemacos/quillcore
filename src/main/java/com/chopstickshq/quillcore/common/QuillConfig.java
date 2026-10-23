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

/** Settings stored in config/quillcore.json. */
public final class QuillConfig {
	public static final int CURRENT_VERSION = 4;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static QuillConfig INSTANCE = new QuillConfig();

	public int configVersion = CURRENT_VERSION;
	public boolean masterEnabled = true;
	public boolean renderEnabled = true;
	public boolean logicEnabled = true;

	public boolean idleAiThrottleEnabled = true;
	public int idleAiThrottleMaxSkipTicks = 2;
	public double idleAiThrottlePlayerRadiusBlocks = 32.0;

	public boolean brainIdleThrottleEnabled = true;
	public int brainIdleThrottleMaxSkipTicks = 2;
	public double brainIdleThrottlePlayerRadiusBlocks = 32.0;

	public boolean entityRenderCutoffEnabled = true;
	public double entityRenderCutoffBlocks = 128.0;

	// signs and skulls only
	public boolean blockEntityRenderCutoffEnabled = true;
	public double blockEntityRenderCutoffBlocks = 96.0;

	public boolean particleRenderCutoffEnabled = true;
	public double particleRenderCutoffBlocks = 64.0;

	public boolean itemMergeThrottleEnabled = true;
	public int itemMergeThrottleMaxSkipTicks = 2;
	public double itemMergeThrottlePlayerRadiusBlocks = 32.0;

	// empty means look on PATH
	public String frameFxExecutablePath = "";

	public boolean pathNavThrottleEnabled = true;
	public int pathNavThrottleMaxSkipTicks = 2;
	public double pathNavThrottlePlayerRadiusBlocks = 32.0;

	public boolean cloudDistanceCapEnabled = false;
	public int cloudDistanceCapChunks = 16;

	public boolean weatherRadiusCapEnabled = false;
	public int weatherRadiusCapBlocks = 6;


	/** Selected upscale backend id. */
	public String upscaleBackendId = "none";
	public boolean frameGenEnabled = false;

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
				loaded.migrate();
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

	public static void resetToDefaults() {
		INSTANCE = new QuillConfig();
		save();
	}

	public void migrate() {
		if (configVersion < 2) {
			configVersion = 2;
		}
		if (configVersion < 3) {
			configVersion = 3;
		}
		if (configVersion < 4) {
			configVersion = 4;
		}
		if (configVersion < CURRENT_VERSION) {
			configVersion = CURRENT_VERSION;
		}
	}

	public void sanitize() {
		configVersion = CURRENT_VERSION;
		idleAiThrottleMaxSkipTicks = IdleAiThrottle.clamp(idleAiThrottleMaxSkipTicks, 0, 10);
		brainIdleThrottleMaxSkipTicks = IdleAiThrottle.clamp(brainIdleThrottleMaxSkipTicks, 0, 10);
		if (Double.isNaN(idleAiThrottlePlayerRadiusBlocks) || idleAiThrottlePlayerRadiusBlocks < 0) {
			idleAiThrottlePlayerRadiusBlocks = 32.0;
		}
		if (Double.isNaN(brainIdleThrottlePlayerRadiusBlocks) || brainIdleThrottlePlayerRadiusBlocks < 0) {
			brainIdleThrottlePlayerRadiusBlocks = 32.0;
		}
		if (Double.isNaN(entityRenderCutoffBlocks) || entityRenderCutoffBlocks < 0) {
			entityRenderCutoffBlocks = 128.0;
		}
		if (Double.isNaN(blockEntityRenderCutoffBlocks) || blockEntityRenderCutoffBlocks < 0) {
			blockEntityRenderCutoffBlocks = 96.0;
		}
		if (Double.isNaN(particleRenderCutoffBlocks) || particleRenderCutoffBlocks < 0) {
			particleRenderCutoffBlocks = 64.0;
		}
		itemMergeThrottleMaxSkipTicks = IdleAiThrottle.clamp(itemMergeThrottleMaxSkipTicks, 0, 10);
		if (Double.isNaN(itemMergeThrottlePlayerRadiusBlocks) || itemMergeThrottlePlayerRadiusBlocks < 0) {
			itemMergeThrottlePlayerRadiusBlocks = 32.0;
		}
		if (frameFxExecutablePath == null) {
			frameFxExecutablePath = "";
		}
		pathNavThrottleMaxSkipTicks = IdleAiThrottle.clamp(pathNavThrottleMaxSkipTicks, 0, 10);
		if (Double.isNaN(pathNavThrottlePlayerRadiusBlocks) || pathNavThrottlePlayerRadiusBlocks < 0) {
			pathNavThrottlePlayerRadiusBlocks = 32.0;
		}
		cloudDistanceCapChunks = IdleAiThrottle.clamp(cloudDistanceCapChunks, 1, 128);
		weatherRadiusCapBlocks = IdleAiThrottle.clamp(weatherRadiusCapBlocks, 1, 10);

		if (upscaleBackendId == null || upscaleBackendId.isBlank()) {
			upscaleBackendId = "none";
		}
		frameGenEnabled = false;
	}

	public String statusSummary() {
		return "master=" + masterEnabled
				+ " render=" + renderEnabled
				+ " logic=" + logicEnabled
				+ " idleAi=" + idleAiThrottleEnabled
				+ " brainAi=" + brainIdleThrottleEnabled
				+ " entCutoff=" + entityRenderCutoffEnabled + "@" + entityRenderCutoffBlocks
				+ " beCutoff=" + blockEntityRenderCutoffEnabled + "@" + blockEntityRenderCutoffBlocks
				+ " particleCutoff=" + particleRenderCutoffEnabled + "@" + particleRenderCutoffBlocks
				+ " itemMerge=" + itemMergeThrottleEnabled
				+ " pathNav=" + pathNavThrottleEnabled
				+ " cloudCap=" + cloudDistanceCapEnabled
				+ " weatherCap=" + weatherRadiusCapEnabled
				+ " upscale=" + upscaleBackendId
				+ " frameGen=" + frameGenEnabled
				+ " bench=" + benchEnabled;
	}
}
