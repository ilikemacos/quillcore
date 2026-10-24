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
 * Every toggle here is wired to real behavior (or an "unavailable" backend status).
 */
public final class QuillConfig {
	public static final int CURRENT_VERSION = 5;

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

	/** Generous default, imperceptible for normal play; purely visual. */
	public boolean entityRenderCutoffEnabled = true;
	public double entityRenderCutoffBlocks = 128.0;

	/** Signs/skulls only; chests and animated BEs never affected. */
	public boolean blockEntityRenderCutoffEnabled = true;
	public double blockEntityRenderCutoffBlocks = 96.0;

	/** Distant particle spawn skip (client-only). Default 64 blocks. */
	public boolean particleRenderCutoffEnabled = true;
	public double particleRenderCutoffBlocks = 64.0;

	public boolean itemMergeThrottleEnabled = true;
	public int itemMergeThrottleMaxSkipTicks = 2;
	public double itemMergeThrottlePlayerRadiusBlocks = 32.0;

	/** Absolute path to Universal-FrameFX.exe (Windows). Empty = PATH probe. */
	public String frameFxExecutablePath = "";

	/** Skip PathNavigation.tick for far idle mobs with no active path. */
	public boolean pathNavThrottleEnabled = true;
	public int pathNavThrottleMaxSkipTicks = 2;
	public double pathNavThrottlePlayerRadiusBlocks = 32.0;

	/** Opt-in: further cap vanilla cloudRange (chunks). Default off. */
	public boolean cloudDistanceCapEnabled = false;
	public int cloudDistanceCapChunks = 16;

	/** Opt-in: cap rain/snow column radius (blocks). Default off. */
	public boolean weatherRadiusCapEnabled = false;
	public int weatherRadiusCapBlocks = 6;


	/**
	 * Selected upscale backend id: none|fsr1|fsr (legacy→fsr1).
	 * Only fsr1/fsr currently run a real path; others refuse with a logged reason.
	 */
	public String upscaleBackendId = "none";
	/** Quality / Balanced / Performance / …, see {@code FsrQualityPreset}. */
	public String fsrQualityPreset = "QUALITY";
	/** RCAS sharpness 0 (sharp) … 2 (soft). Default 0.2. */
	public float fsrSharpness = 0.2f;
	/**
	 * Optional manual render scale (0 = use preset). Clamped to 0.25–1.0 when > 0.
	 * Values ≥ 1 skip FSR1 (no upscale).
	 */
	public float fsrRenderScaleOverride = 0.0f;
	/** FSR3 frame generation, accepted in config but never enabled (no FG backend). */
	public boolean fsrFrameGenEnabled = false;
	/** Legacy / generic frame-gen flag; forced off, no working FG backend. */
	public boolean frameGenEnabled = false;

	public boolean benchEnabled = false;
	/**
	 * System RAM (GB) used to park chunk meshes evicted from VRAM. 0 = off. This is not extra VRAM;
	 * it only lets evicted sections come back without a rebuild. See {@code MeshRamCache}.
	 */
	public double meshRamCacheGb = 0.0;

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

	/** Bring older config files forward without wiping user toggles. */
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
			// Prefer explicit fsr1 id over legacy "fsr".
			if ("fsr".equalsIgnoreCase(upscaleBackendId)) {
				upscaleBackendId = "fsr1";
			}
			configVersion = 5;
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
		if (Double.isNaN(meshRamCacheGb) || meshRamCacheGb < 0) meshRamCacheGb = 0;
		if (meshRamCacheGb > 16) meshRamCacheGb = 16;
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
		// No working frame-gen backend, keep generic flag off. fsrFrameGenEnabled may be stored but is ignored.
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
				+ " bench=" + benchEnabled;
	}
}
