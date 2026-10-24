package com.chopstickshq.quillcore.gametest;

import com.chopstickshq.quillcore.bench.BenchHarness;
import com.chopstickshq.quillcore.bench.BenchMetrics;
import com.chopstickshq.quillcore.bench.BenchReport;
import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.render.mesh.SectionFootprintSampler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.nio.file.Path;

/**
 * Automated client benchmark, enabled only with {@code -Dquillcore.autobench=<secondsPerSegment>}.
 * Creates a fixed-seed world, warms up, then alternates QuillCore on/off segments (on,off,on,off)
 * while slowly panning the camera, sampling real frame times + integrated-server MSPT via
 * {@link BenchHarness}. Writes bench.json / bench.csv to {@code -Dquillcore.autobench.out} and quits.
 */
public final class QuillAutoBench implements ClientModInitializer {
	private static final String[] SEGMENTS = {"on", "off", "on", "off"};
	private static final int WARMUP_TICKS = 20 * 20;
	private static final int TIMEOUT_TICKS = 20 * 240;

	private int segTicks;
	private boolean worldRequested;
	private int ticks;
	private int inWorldTicks;
	/** True while the autobench is armed; read by FramerateLimitTrackerMixin to disable throttling. */
	public static volatile boolean ACTIVE;
	private int unfocusedTicks, iconifiedTicks;
	private int segment = -1;
	private int segmentStart;
	private boolean done;
	private final BenchReport report = new BenchReport();
	/** Computed once per run; synthetic, so identical for on/off segments. */
	private SectionFootprintSampler.Footprint footprint;

	@Override
	public void onInitializeClient() {
		String prop = System.getProperty("quillcore.autobench");
		if (prop == null) return;
		segTicks = Math.max(5, Integer.parseInt(prop.trim())) * 20;
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		ACTIVE = true;
		QuillCore.LOGGER.info("[quill-bench] autobench armed: {}s per segment", segTicks / 20);
	}

	private void tick(Minecraft mc) {
		if (done) return;
		ticks++;
		if (!worldRequested && mc.level == null && (mc.screen instanceof TitleScreen
				|| mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)) {
			worldRequested = true;
			mc.options.enableVsync().set(false);
			mc.options.framerateLimit().set(260);
			mc.options.renderDistance().set(12);
			mc.options.simulationDistance().set(8);
			mc.options.pauseOnLostFocus = false;
			// AFK limit would drop the game to 30 fps after a minute without input and skew segments.
			mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
			if (!Boolean.getBoolean("quillcore.autobench.windowed")) {
				mc.options.fullscreen().set(true);
				if (!mc.getWindow().isFullscreen()) mc.getWindow().toggleFullScreen();
			}
			mc.options.save();
			LevelSettings settings = new LevelSettings("QuillBench", GameType.CREATIVE, false, Difficulty.NORMAL, true,
					new GameRules(FeatureFlags.DEFAULT_FLAGS), WorldDataConfiguration.DEFAULT);
			mc.createWorldOpenFlows().createFreshLevel("quillbench-" + System.currentTimeMillis(), settings,
					new WorldOptions(20261023L, true, false), WorldPresets::createNormalWorldDimensions, mc.screen);
			return;
		}
		if (mc.level == null || mc.player == null) {
			if (ticks > TIMEOUT_TICKS) finish(mc, "timeout: world never loaded");
			return;
		}
		if (mc.screen != null && mc.screen.isPauseScreen()) mc.setScreen(null);
		inWorldTicks++;
		mc.player.setYRot(mc.player.getYRot() + 0.75f);
		if (inWorldTicks < WARMUP_TICKS) return;
		int t = inWorldTicks - WARMUP_TICKS;
		int idx = t / segTicks;
		if (segment >= 0) {
			if (!mc.isWindowActive()) unfocusedTicks++;
			if (mc.getWindow().isIconified()) iconifiedTicks++;
		}
		if (idx != segment) {
			if (segment >= 0) {
				BenchHarness.stop();
				BenchMetrics m = BenchHarness.snapshot();
				if (QuillConfig.get().vertexFootprintSampleEnabled) {
					if (footprint == null) footprint = SectionFootprintSampler.sampleSolidSectionShell();
					m.vanillaVertexBytes = footprint.vanillaBytes();
					m.quillVertexBytes = footprint.quillBytes();
					m.vertexByteReductionPct = footprint.reductionPct();
					m.notes = m.notes + "; vertex-footprint-only; live mesh path unchanged";
				}
				report.add(SEGMENTS[segment], m, BenchHarness.frameSamplesCopy());
				QuillCore.LOGGER.info("[quill-bench] {} -> {}", SEGMENTS[segment], BenchHarness.report());
			}
			segment = idx;
			if (segment >= SEGMENTS.length) {
				finish(mc, null);
				return;
			}
			QuillConfig.get().masterEnabled = "on".equals(SEGMENTS[segment]);
			BenchHarness.start(SEGMENTS[segment]);
		}
	}

	private void finish(Minecraft mc, String error) {
		done = true;
		QuillConfig.get().masterEnabled = true;
		String gpu = "unknown";
		String glVersion = "unknown";
		try {
			gpu = com.mojang.blaze3d.systems.RenderSystem.getDevice().getRenderer();
			glVersion = com.mojang.blaze3d.systems.RenderSystem.getDevice().getVersion();
		} catch (Throwable ignored) {
		}
		gpu = gpu + "; glVersion=" + glVersion;
		report.environment((error == null ? "" : "ERROR " + error + "; ") + "gpu=" + gpu + "; os="
				+ System.getProperty("os.name") + "; java=" + System.getProperty("java.version")
				+ "; resolution=" + mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight()
				+ "; fullscreen=" + mc.getWindow().isFullscreen() + "; segmentSeconds=" + segTicks / 20 + "; seed=20261023; renderDistance=12; vsync=off; cap=260; throttling=disabled; unfocusedTicks=" + unfocusedTicks + "; iconifiedTicks=" + iconifiedTicks);
		try {
			Path out = Path.of(System.getProperty("quillcore.autobench.out", "quill-test-report"));
			report.write(out);
			QuillCore.LOGGER.info("[quill-bench] wrote {}", out.toAbsolutePath());
		} catch (Exception e) {
			QuillCore.LOGGER.error("[quill-bench] failed to write report", e);
		}
		mc.stop();
	}
}
