package com.chopstickshq.quillcore.gametest;

import com.chopstickshq.quillcore.bench.BenchHarness;
import com.chopstickshq.quillcore.bench.BenchReport;
import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
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
 * Client benchmark, enabled with -Dquillcore.autobench=<seconds per segment>.
 * Creates a fixed-seed world, warms up, then runs on/off/on/off segments while
 * turning the camera, writes bench.json and bench.csv and quits.
 */
public final class QuillAutoBench implements ClientModInitializer {
	private static final String[] SEGMENTS = {"on", "off", "on", "off"};
	private static final int WARMUP_TICKS = 20 * 20;
	private static final int TIMEOUT_TICKS = 20 * 240;

	private int segTicks;
	private boolean worldRequested;
	private int ticks;
	private int inWorldTicks;
	private int segment = -1;
	private int segmentStart;
	private boolean done;
	private final BenchReport report = new BenchReport();

	@Override
	public void onInitializeClient() {
		String prop = System.getProperty("quillcore.autobench");
		if (prop == null) return;
		segTicks = Math.max(5, Integer.parseInt(prop.trim())) * 20;
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
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
			// the AFK limit drops to 30 fps after a minute without input
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
		if (idx != segment) {
			if (segment >= 0) {
				BenchHarness.stop();
				report.add(SEGMENTS[segment], BenchHarness.snapshot(), BenchHarness.frameSamplesCopy());
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
				+ "; fullscreen=" + mc.getWindow().isFullscreen() + "; segmentSeconds=" + segTicks / 20 + "; seed=20261023; renderDistance=12; vsync=off; cap=260; inactivityFpsLimit=minimized");
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
