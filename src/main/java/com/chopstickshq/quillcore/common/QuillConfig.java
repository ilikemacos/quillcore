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
	public static final int CURRENT_VERSION = 6;

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

	// none or fsr1 ("fsr" is the old name for fsr1)
	public String upscaleBackendId = "none";
	public String fsrQualityPreset = "QUALITY";
	// 0 = sharpest, 2 = softest
	public float fsrSharpness = 0.2f;
	// 0 = use the preset, 1 = no upscale
	public float fsrRenderScaleOverride = 0.0f;
	// unused, kept so old config files still parse
	public boolean fsrFrameGenEnabled = false;
	public boolean frameGenEnabled = false;

	/** Optional CSR Flux 2x upscale (renders the world at half resolution per axis). Off by default. See docs/csr.md. */
	public boolean csrFlux = false;
	/** Optional CSR Frame Gen (interpolated frames, adds about one frame of latency). Off by default. See docs/csr.md. */
	public boolean csrFrameGen = false;

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
		if (configVersion < 5) {
			if ("fsr".equalsIgnoreCase(upscaleBackendId)) {
				upscaleBackendId = "fsr1";
			}
			configVersion = 5;
		}
		if (configVersion < 6) {
			// v6 adds csrFlux and csrFrameGen, both off when missing
			configVersion = 6;
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
		} else {
			upscaleBackendId = upscaleBackendId.trim().toLowerCase();
			if ("fsr".equals(upscaleBackendId)) {
				upscaleBackendId = "fsr1";
			}
		}
		if (fsrQualityPreset == null || fsrQualityPreset.isBlank()) {
			fsrQualityPreset = "QUALITY";
		}
		if (Float.isNaN(fsrSharpness) || fsrSharpness < 0.0f || fsrSharpness > 2.0f) {
			fsrSharpness = 0.2f;
		}
		if (Float.isNaN(fsrRenderScaleOverride) || fsrRenderScaleOverride < 0.0f) {
			fsrRenderScaleOverride = 0.0f;
		} else if (fsrRenderScaleOverride > 1.0f) {
			fsrRenderScaleOverride = 1.0f;
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
				+ " fsrPreset=" + fsrQualityPreset
				+ " fsrFG=" + fsrFrameGenEnabled
				+ " frameGen=" + frameGenEnabled
				+ " csrFlux=" + csrFlux
				+ " csrFrameGen=" + csrFrameGen
				+ " bench=" + benchEnabled;
	}
}
