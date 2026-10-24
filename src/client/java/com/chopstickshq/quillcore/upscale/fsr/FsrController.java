package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.render.ConflictingRendererCheck;
import com.chopstickshq.quillcore.upscale.UpscaleBackend;
import com.chopstickshq.quillcore.upscale.UpscaleRegistry;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Shrinks the main target before the world pass and upscales it before the GUI. */
public final class FsrController {
	private static Fsr1Upscaler upscaler;
	private static boolean pipelinesReady;
	private static boolean conflictWarned;
	private static boolean activeThisFrame;
	private static int nativeW;
	private static int nativeH;
	private static int scaledW;
	private static int scaledH;

	private FsrController() {}

	public static void bootstrap() {
		try {
			FsrPipelines.bootstrap();
			pipelinesReady = true;
			QuillCore.LOGGER.info("FSR1 pipelines registered");
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

	public static void beforeWorldRender() {
		activeThisFrame = false;
		if (!shouldRun()) {
			return;
		}
		warnConflictOnce();
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

			// resizing main clears it, so the upscaler renders into its own targets first
			upscaler.upscaleIntoRestoredMain(main, scaledW, scaledH, nativeW, nativeH, sharpness);
		} catch (Throwable t) {
			upscaler.markFailed("afterWorldBeforeGui", t);
			restoreMain(main, nativeW, nativeH);
		}
	}

	private static void warnConflictOnce() {
		if (conflictWarned) {
			return;
		}
		conflictWarned = true;
		List<String> loaded = ConflictingRendererCheck.findLoaded(FabricLoader.getInstance()::isModLoaded);
		if (!loaded.isEmpty()) {
			QuillCore.LOGGER.error("Incompatible renderer loaded: {}. Remove it, QuillCore replaces the chunk renderer.", loaded);
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
			return "FSR1 failed, disabled until restart";
		}
		if (isFsr1Selected() && shouldRun()) {
			FsrQualityPreset p = FsrQualityPreset.fromId(QuillConfig.get().fsrQualityPreset);
			return "FSR1 active preset=" + p.name() + " scale=" + p.renderScale();
		}
		if (isFsr1Selected()) {
			return "FSR1 selected (idle)";
		}
		return "FSR1 idle";
	}
}
