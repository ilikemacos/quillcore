package com.chopstickshq.quillcore.csr.client;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.csr.CsrFeature;
import com.chopstickshq.quillcore.csr.CsrFgWeights;
import com.chopstickshq.quillcore.csr.CsrFluxWeights;
import com.chopstickshq.quillcore.csr.CsrPacing;
import com.chopstickshq.quillcore.csr.CsrResult;
import com.chopstickshq.quillcore.csr.CsrWeightsLoader;
import com.chopstickshq.quillcore.upscale.fsr.FsrController;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Frame-level hooks for the optional CSR add-on. Everything is off unless the config flags are
 * set; any failure disables the feature for the session (one warning) and the frame renders vanilla.
 */
public final class CsrController {
	private static final CsrFeature FLUX = new CsrFeature("CSR Flux", QuillCore.LOGGER::warn);
	private static final CsrFeature FRAME_GEN = new CsrFeature("CSR Frame Gen", QuillCore.LOGGER::warn);
	public static final String FRAME_GEN_NOTE = "CSR Frame Gen: +1 frame latency, interpolated frames";

	private static CsrResult<CsrFgWeights> fgWeights;
	private static CsrFrameGen frameGen;
	private static boolean fgAnnounced;
	private static CsrPacing.Mode fgLastPacingMode;

	private static CsrResult<CsrFluxWeights> fluxWeights;
	private static CsrFlux flux;
	private static boolean fluxActiveThisFrame;
	private static boolean fluxConflictWarned;
	private static boolean fluxAnnounced;
	private static int nativeW;
	private static int nativeH;
	private static int lrW;
	private static int lrH;

	private CsrController() {}

	public static void bootstrap() {
		HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath(QuillCore.MOD_ID, "csr_frame_gen_note"),
				(graphics, tick) -> drawFrameGenNote(graphics));
	}

	// ---- CSR Flux: render-scale 2x ----

	private static boolean fluxRequested() {
		QuillConfig c = QuillConfig.get();
		return c.masterEnabled && c.csrFlux;
	}

	/** Before {@code renderLevel}: shrink the main target to half size per axis. */
	public static void beforeWorldRender() {
		fluxActiveThisFrame = false;
		if (!fluxRequested() || FLUX.isDisabled()) {
			releaseFlux();
			return;
		}
		if (FsrController.isFsr1Selected()) {
			if (!fluxConflictWarned) {
				fluxConflictWarned = true;
				QuillCore.LOGGER.warn("CSR Flux not started: the FSR1 upscale backend is selected and both resize the main target. Choose one.");
			}
			return;
		}
		fluxConflictWarned = false;
		Minecraft mc = Minecraft.getInstance();
		RenderTarget main = mc.getMainRenderTarget();
		if (mc.level == null || main == null) {
			return;
		}
		nativeW = mc.getWindow().getWidth();
		nativeH = mc.getWindow().getHeight();
		lrW = (nativeW + 1) / 2;
		lrH = (nativeH + 1) / 2;
		if (lrW < 16 || lrH < 16) {
			return;
		}
		try {
			if (flux == null && !initFlux()) {
				return;
			}
			if (main.width != lrW || main.height != lrH) {
				main.resize(lrW, lrH);
			}
			fluxActiveThisFrame = true;
		} catch (Throwable t) {
			failFlux("resize before world render", t, main);
		}
	}

	private static boolean initFlux() {
		if (fluxWeights == null) {
			fluxWeights = CsrWeightsLoader.loadFlux();
		}
		CsrFluxWeights w = FLUX.tryEnable(true, fluxWeights);
		if (w == null) {
			return false;
		}
		String caps = CsrGl.capsProblem();
		if (caps != null) {
			FLUX.disable(caps);
			return false;
		}
		try {
			flux = new CsrFlux(w);
		} catch (Throwable t) {
			FLUX.disable("GPU setup failed", t);
			return false;
		}
		if (!fluxAnnounced) {
			fluxAnnounced = true;
			QuillCore.LOGGER.info("CSR Flux enabled: the world renders at half width and height and is upscaled 2x "
					+ "by the CSR network (GLSL, ~1512 MACs per low-res pixel). HUD stays at native resolution.");
		}
		return true;
	}

	/** After world + post, before GUI: run the network, restore the native main target, blit the result in. */
	public static void afterWorldBeforeGui() {
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (fluxActiveThisFrame) {
			fluxActiveThisFrame = false;
			try {
				flux.runNetwork(main, lrW, lrH);
				main.resize(nativeW, nativeH);
				flux.blitInto(main);
			} catch (Throwable t) {
				failFlux("upscale", t, main);
			}
		}
		if (frameGen != null && frameGenRequested()) {
			try {
				frameGen.captureWorld(main);
			} catch (Throwable t) {
				failFrameGen("world capture", t);
			}
		}
	}

	private static void failFlux(String where, Throwable t, RenderTarget main) {
		fluxActiveThisFrame = false;
		FLUX.disable(where, t);
		QuillCore.LOGGER.debug("CSR Flux failure detail", t);
		restoreMain(main);
		releaseFlux();
	}

	private static void restoreMain(RenderTarget main) {
		try {
			if (main != null && nativeW > 0 && nativeH > 0 && (main.width != nativeW || main.height != nativeH)) {
				main.resize(nativeW, nativeH);
			}
		} catch (Throwable t) {
			QuillCore.LOGGER.error("Failed to restore main render target to {}x{}", nativeW, nativeH, t);
		}
	}

	private static void releaseFlux() {
		if (flux == null) {
			return;
		}
		try {
			flux.close();
		} catch (Throwable t) {
			QuillCore.LOGGER.debug("CSR Flux release failed", t);
		}
		flux = null;
	}

	// ---- CSR Frame Gen ----

	private static boolean frameGenRequested() {
		QuillConfig c = QuillConfig.get();
		return c.masterEnabled && c.csrFrameGen;
	}

	/** In {@code Minecraft.runTick}, right before the vanilla {@code Window.updateDisplay} swap. */
	public static void beforePresent() {
		if (!frameGenRequested() || FRAME_GEN.isDisabled()) {
			releaseFrameGen();
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		RenderTarget main = mc.getMainRenderTarget();
		if (mc.level == null || main == null || mc.getWindow().isMinimized()) {
			if (frameGen != null) {
				frameGen.reset();
			}
			return;
		}
		try {
			if (frameGen == null && !initFrameGen()) {
				return;
			}
			boolean vsync = mc.options.enableVsync().get();
			int limit = mc.getFramerateLimitTracker().getFramerateLimit();
			logPacingMode(CsrPacing.mode(vsync, limit));
			frameGen.present(main, mc.getWindow().getWindow(),
					mc.getWindow().getWidth(), mc.getWindow().getHeight(), CsrPacing.gapNanos(vsync, limit));
		} catch (Throwable t) {
			failFrameGen("present", t);
		}
	}

	private static boolean initFrameGen() {
		if (fgWeights == null) {
			fgWeights = CsrWeightsLoader.loadFrameGen();
		}
		CsrFgWeights w = FRAME_GEN.tryEnable(true, fgWeights);
		if (w == null) {
			return false;
		}
		String caps = CsrGl.capsProblem();
		if (caps != null) {
			FRAME_GEN.disable(caps);
			return false;
		}
		try {
			frameGen = new CsrFrameGen(w);
		} catch (Throwable t) {
			FRAME_GEN.disable("GPU setup failed", t);
			return false;
		}
		if (!fgAnnounced) {
			fgAnnounced = true;
			QuillCore.LOGGER.warn("CSR Frame Gen enabled. It shows one interpolated frame between each pair of real frames. "
					+ "Latency: each real frame is held back until the interpolated frame before it has been computed and "
					+ "presented, so input-to-photon latency grows by up to about one real frame. It does not simulate extra "
					+ "frames, does not reduce input latency, and does not change the FPS counter (F3 shows real frames only).");
		}
		return true;
	}

	private static void logPacingMode(CsrPacing.Mode mode) {
		if (mode == fgLastPacingMode) {
			return;
		}
		fgLastPacingMode = mode;
		switch (mode) {
			case VSYNC -> QuillCore.LOGGER.info("CSR Frame Gen pacing: VSync on; the driver spaces both presents at vblank.");
			case CAPPED -> QuillCore.LOGGER.info("CSR Frame Gen pacing: FPS cap; the real frame waits half the cap period after the interpolated one. This lowers the real frame rate when the cap is not already the bottleneck.");
			case NONE -> QuillCore.LOGGER.warn("CSR Frame Gen pacing: VSync off and FPS unlimited, so the interpolated and real frames are presented back to back. Expect no smoothness gain. Turn on VSync or set an FPS cap.");
		}
	}

	private static void failFrameGen(String where, Throwable t) {
		FRAME_GEN.disable(where, t);
		QuillCore.LOGGER.debug("CSR Frame Gen failure detail", t);
		releaseFrameGen();
	}

	private static void releaseFrameGen() {
		if (frameGen == null) {
			return;
		}
		try {
			frameGen.close();
		} catch (Throwable t) {
			QuillCore.LOGGER.debug("CSR Frame Gen release failed", t);
		}
		frameGen = null;
	}

	/** True while frame gen is running, so the on-screen note is shown. */
	public static boolean frameGenActive() {
		return frameGen != null && frameGenRequested() && !FRAME_GEN.isDisabled();
	}

	private static void drawFrameGenNote(GuiGraphics g) {
		if (!frameGenActive()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int x = g.guiWidth() - mc.font.width(FRAME_GEN_NOTE) - 2;
		g.drawString(mc.font, FRAME_GEN_NOTE, Math.max(2, x), 2, 0xC0FFFFFF, true);
	}

	public static String runtimeStatus() {
		return "Flux " + featureStatus(FLUX, fluxRequested(), flux != null)
				+ ", Frame Gen " + featureStatus(FRAME_GEN, frameGenRequested(), frameGen != null);
	}

	private static String featureStatus(CsrFeature f, boolean requested, boolean running) {
		if (f.isDisabled()) {
			return "disabled (" + f.reason() + ")";
		}
		if (!requested) {
			return "off";
		}
		return running ? "active" : "on (idle)";
	}
}
