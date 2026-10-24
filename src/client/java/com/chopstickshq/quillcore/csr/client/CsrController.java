package com.chopstickshq.quillcore.csr.client;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.csr.CsrFeature;
import com.chopstickshq.quillcore.csr.CsrFluxWeights;
import com.chopstickshq.quillcore.csr.CsrResult;
import com.chopstickshq.quillcore.csr.CsrWeightsLoader;
import com.chopstickshq.quillcore.upscale.fsr.FsrController;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

/**
 * Frame-level hooks for the optional CSR add-on. Everything is off unless the config flags are
 * set; any failure disables the feature for the session (one warning) and the frame renders vanilla.
 */
public final class CsrController {
	private static final CsrFeature FLUX = new CsrFeature("CSR Flux", QuillCore.LOGGER::warn);

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
		if (!fluxActiveThisFrame) {
			return;
		}
		fluxActiveThisFrame = false;
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		try {
			flux.runNetwork(main, lrW, lrH);
			main.resize(nativeW, nativeH);
			flux.blitInto(main);
		} catch (Throwable t) {
			failFlux("upscale", t, main);
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

	public static String runtimeStatus() {
		return "Flux " + featureStatus(FLUX, fluxRequested(), flux != null);
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
