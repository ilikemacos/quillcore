package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.upscale.UpscaleBackend;
import com.chopstickshq.quillcore.upscale.UpscaleRegistry;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Orchestrates FSR1 main-target scale + EASU/RCAS around {@code GameRenderer#render}.
 * FSR2/FSR3 selections are refused with a logged reason.
 */
public final class FsrController {
	private static Fsr1Upscaler upscaler;
	private static boolean pipelinesReady;
	private static boolean sodiumWarned;
	private static boolean activeThisFrame;
	private static int nativeW;
	private static int nativeH;
	private static int scaledW;
	private static int scaledH;
	private static String lastRefuseLog = "";

	private FsrController() {}

	public static void bootstrap() {
		try {
			FsrPipelines.bootstrap();
			pipelinesReady = true;
			QuillCore.LOGGER.info("FSR1 pipelines registered (EASU+RCAS). AMD FidelityFX FSR1 MIT — see third_party/amd-fsr1/");
		} catch (Throwable t) {
			pipelinesReady = false;
			QuillCore.LOGGER.error("FSR1 pipeline registration failed", t);
		}
	}

	public static boolean isFsr1Selected() {
		QuillConfig c = QuillConfig.get();
		if (!c.masterEnabled) {
			return false;
		}
		String id = c.upscaleBackendId == null ? "none" : c.upscaleBackendId.trim().toLowerCase();
		return "fsr1".equals(id) || "fsr".equals(id);
	}

	public static boolean shouldRun() {
		if (!pipelinesReady || !isFsr1Selected()) {
			return false;
		}
		UpscaleBackend b = UpscaleRegistry.get(QuillConfig.get().upscaleBackendId);
		if (b != null && !b.isAvailable()) {
			return false;
		}
		if (upscaler != null && upscaler.hasFailed()) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.getMainRenderTarget() != null;
	}

	/** Call before {@code renderLevel}. Shrinks main RT when FSR1 is active. */
	public static void beforeWorldRender() {
		activeThisFrame = false;
		refuseNonFsr1IfSelected();
		if (!shouldRun()) {
			return;
		}
		warnSodiumOnce();
		Minecraft mc = Minecraft.getInstance();
		RenderTarget main = mc.getMainRenderTarget();
		nativeW = mc.getWindow().getWidth();
		nativeH = mc.getWindow().getHeight();
		if (nativeW <= 0 || nativeH <= 0) {
			return;
		}
		FsrQualityPreset preset = FsrQualityPreset.fromId(QuillConfig.get().fsrQualityPreset);
		float scale = QuillConfig.get().fsrRenderScaleOverride > 0.0f
				? clampScale(QuillConfig.get().fsrRenderScaleOverride)
				: preset.renderScale();
		scaledW = FsrQualityPreset.scaleDimension(nativeW, scale);
		scaledH = FsrQualityPreset.scaleDimension(nativeH, scale);
		if (scaledW >= nativeW && scaledH >= nativeH) {
			// No upscale benefit at ≥1.0 — skip (still honest: not running FSR).
			return;
		}
		try {
			if (upscaler == null) {
				upscaler = new Fsr1Upscaler();
			}
			if (main.width != scaledW || main.height != scaledH) {
				main.resize(scaledW, scaledH);
			}
			activeThisFrame = true;
		} catch (Throwable t) {
			if (upscaler != null) {
				upscaler.markFailed("beforeWorldRender resize", t);
			} else {
				QuillCore.LOGGER.error("FSR1 beforeWorldRender failed", t);
			}
			restoreMain(main, nativeW, nativeH);
			activeThisFrame = false;
		}
	}

	/** Call after world/post, before GUI. Upscales and restores native main RT. */
	public static void afterWorldBeforeGui() {
		if (!activeThisFrame) {
			return;
		}
		activeThisFrame = false;
		Minecraft mc = Minecraft.getInstance();
		RenderTarget main = mc.getMainRenderTarget();
		try {
			float sharpness = QuillConfig.get().fsrSharpness;
			if (Float.isNaN(sharpness)) {
				sharpness = 0.2f;
			}
			sharpness = Math.max(0.0f, Math.min(2.0f, sharpness));

			// Upscale into intermediate full-res targets, then restore main and blit.
			// Resize destroys main contents — upscaler writes into its own targets first,
			// then we resize main and blit the RCAS result in.
			upscaler.upscaleIntoRestoredMain(main, scaledW, scaledH, nativeW, nativeH, sharpness);
		} catch (Throwable t) {
			upscaler.markFailed("afterWorldBeforeGui", t);
			restoreMain(main, nativeW, nativeH);
		}
	}

	private static void refuseNonFsr1IfSelected() {
		QuillConfig c = QuillConfig.get();
		if (!c.masterEnabled) {
			return;
		}
		String id = c.upscaleBackendId == null ? "none" : c.upscaleBackendId.trim().toLowerCase();
		if ("fsr2".equals(id) || "fsr3".equals(id)) {
			UpscaleBackend b = UpscaleRegistry.get(id);
			String reason = b != null ? b.unavailableReason() : "unavailable";
			if (!reason.equals(lastRefuseLog)) {
				lastRefuseLog = reason;
				QuillCore.LOGGER.warn("Upscale backend '{}' refused: {}", id, reason);
			}
		}
		if (c.fsrFrameGenEnabled) {
			if (!"fg".equals(lastRefuseLog)) {
				lastRefuseLog = "fg";
				QuillCore.LOGGER.warn("FSR3 frame generation requested but disabled — no FG backend.");
			}
		}
	}

	private static void warnSodiumOnce() {
		if (sodiumWarned) {
			return;
		}
		sodiumWarned = true;
		if (FabricLoader.getInstance().isModLoaded("sodium")) {
			QuillCore.LOGGER.warn(
					"Sodium detected: FSR1 uses vanilla main RenderTarget resize. "
							+ "If world resolution does not change with Sodium, FSR1 is limited/disabled with Sodium — see docs/FEASIBILITY.md."
			);
		}
	}

	private static float clampScale(float s) {
		if (Float.isNaN(s) || s <= 0.0f) {
			return FsrQualityPreset.QUALITY.renderScale();
		}
		return Math.max(0.25f, Math.min(1.0f, s));
	}

	private static void restoreMain(RenderTarget main, int w, int h) {
		try {
			if (main != null && (main.width != w || main.height != h) && w > 0 && h > 0) {
				main.resize(w, h);
			}
		} catch (Throwable t) {
			QuillCore.LOGGER.error("Failed to restore main RenderTarget to {}x{}", w, h, t);
		}
	}

	public static String runtimeStatus() {
		if (!pipelinesReady) {
			return "FSR1 pipelines not registered";
		}
		if (upscaler != null && upscaler.hasFailed()) {
			return "FSR1 failed (see log) — disabled for session";
		}
		if (isFsr1Selected() && shouldRun()) {
			FsrQualityPreset p = FsrQualityPreset.fromId(QuillConfig.get().fsrQualityPreset);
			return "FSR1 active preset=" + p.name() + " scale=" + p.renderScale();
		}
		if (isFsr1Selected()) {
			return "FSR1 selected (idle — not in world or scale≥1)";
		}
		return "FSR1 idle";
	}
}
