package com.chopstickshq.quillcore.gametest;

import com.chopstickshq.quillcore.bench.BenchHarness;
import com.chopstickshq.quillcore.bench.BenchMetrics;
import com.chopstickshq.quillcore.bench.BenchReport;
import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.render.mesh.QuillMeshStats;
import net.minecraft.client.Screenshot;
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
	private boolean done;
	private final BenchReport report = new BenchReport();

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
			// 260 is the slider's top stop, which vanilla treats as "Unlimited" (no frame limiter runs).
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
		if (inWorldTicks < WARMUP_TICKS) {
			mc.player.setYRot(mc.player.getYRot() + 0.75f);
			return;
		}
		if (segment >= 0 && measuring) {
			if (!mc.isWindowActive()) unfocusedTicks++;
			if (mc.getWindow().isIconified()) iconifiedTicks++;
		}
		// Phase 1: fixed-camera mesh compare (vanilla SOLID mesh vs Quill compact SOLID mesh, everything else equal).
		if (compareStep < COMPARE_SHOTS.length) {
			compareTick(mc);
			return;
		}
		if (pendingShots > 0) return;
		mc.player.setYRot(mc.player.getYRot() + 0.75f);
		if (segment < 0 || (measuring && ++segTick >= segTicks)) {
			if (segment >= 0) endSegment();
			segment++;
			if (segment >= SEGMENTS.length) {
				finish(mc, null);
				return;
			}
			boolean on = "on".equals(SEGMENTS[segment]);
			QuillConfig.get().masterEnabled = on;
			QuillConfig.get().compactMeshEnabled = on;
			QuillMeshStats.reset();
			mc.levelRenderer.allChanged();
			measuring = false;
			settledTicks = 0;
			waitTicks = 0;
			return;
		}
		if (!measuring) {
			if (settled(mc) || ++waitTicks > REBUILD_TIMEOUT_TICKS) {
				measuring = true;
				segTick = 0;
				segRebuildTicks = waitTicks;
				BenchHarness.start(SEGMENTS[segment]);
			}
		}
	}

	private static final String[] COMPARE_SHOTS = {"vanilla-mesh", "quill-mesh"};
	private static final int REBUILD_TIMEOUT_TICKS = 20 * 30;
	private int compareStep;
	private int compareTicks;
	private volatile int pendingShots;
	private boolean measuring;
	private int segTick, settledTicks, waitTicks, segRebuildTicks;
	private final int[][] shots = new int[COMPARE_SHOTS.length][];
	private final int[] shotW = new int[COMPARE_SHOTS.length];
	private Double meanAbsRgbDiff;
	private double maxChannelDiffShare;
	private QuillMeshStats.Snapshot compareStats;
	private final java.util.List<String> segmentStats = new java.util.ArrayList<>();
	private final java.util.Map<String, QuillMeshStats.Snapshot> statsByLabel = new java.util.LinkedHashMap<>();

	/** All visible sections compiled and uploaded for {@code 20} consecutive ticks. */
	private boolean settled(Minecraft mc) {
		var d = mc.levelRenderer.getSectionRenderDispatcher();
		boolean idle = d != null && d.isQueueEmpty() && d.getToUpload() == 0 && mc.levelRenderer.hasRenderedAllSections();
		settledTicks = idle ? settledTicks + 1 : 0;
		return settledTicks >= 20;
	}

	private void compareTick(Minecraft mc) {
		if (compareTicks == 0) {
			if (compareStep == 0) {
				savedClouds = mc.options.cloudStatus().get();
				mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
				freezeSky(mc);
			}
			mc.player.setYRot(COMPARE_YAW);
			mc.player.setXRot(COMPARE_PITCH);
			QuillConfig.get().masterEnabled = true;
			QuillConfig.get().compactMeshEnabled = compareStep == 1;
			QuillMeshStats.reset();
			mc.levelRenderer.allChanged();
			settledTicks = 0;
		}
		compareTicks++;
		mc.player.setYRot(COMPARE_YAW);
		mc.player.setXRot(COMPARE_PITCH);
		if (!(settled(mc) || compareTicks > REBUILD_TIMEOUT_TICKS)) return;
		int step = compareStep;
		if (step == 1) {
			compareStats = QuillMeshStats.snapshot();
			QuillCore.LOGGER.info("[quill-bench] region slabs: {}, solid draws {} -> {}", com.chopstickshq.quillcore.render.region.RegionSlabs.count(),
					com.chopstickshq.quillcore.render.region.RegionDraws.lastIn(), com.chopstickshq.quillcore.render.region.RegionDraws.lastOut());
		}
		pendingShots++;
		Screenshot.takeScreenshot(mc.getMainRenderTarget(), img -> {
			shots[step] = img.getPixels();
			shotW[step] = img.getWidth();
			try {
				Path dir = outDir().resolve("screens");
				java.nio.file.Files.createDirectories(dir);
				img.writeToFile(dir.resolve(COMPARE_SHOTS[step] + ".png"));
			} catch (Exception e) {
				QuillCore.LOGGER.error("[quill-bench] screenshot write failed", e);
			}
			if (shots[0] != null && shots[1] != null) computeDiff();
			pendingShots--;
		});
		compareStep++;
		compareTicks = 0;
		if (compareStep == COMPARE_SHOTS.length) {
			mc.options.cloudStatus().set(savedClouds);
		}
	}

	/** Both shots must see the same sun and weather, or the daylight drift between them shows up as mesh error. */
	private static void freezeSky(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		server.execute(() -> {
			var level = server.overworld();
			level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
			level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, server);
			level.setDayTime(6000);
			level.setWeatherParameters(6000, 0, false, false);
		});
	}

	private static final float COMPARE_YAW = 45f, COMPARE_PITCH = 20f;
	private net.minecraft.client.CloudStatus savedClouds = net.minecraft.client.CloudStatus.FANCY;

	private void computeDiff() {
		int[] a = shots[0], b = shots[1];
		if (a.length != b.length || shotW[0] != shotW[1]) return;
		long sum = 0, big = 0, n = 0;
		{
			for (int i = 0; i < a.length; i++) {
				int pa = a[i], pb = b[i];
				for (int sh = 0; sh <= 16; sh += 8) {
					int d = Math.abs(((pa >> sh) & 0xFF) - ((pb >> sh) & 0xFF));
					sum += d;
					if (d > 16) big++;
					n++;
				}
			}
		}
		meanAbsRgbDiff = (double) sum / n;
		maxChannelDiffShare = (double) big / n;
		QuillCore.LOGGER.info("[quill-bench] mesh compare: meanAbsRgbDiff={} /255, channels >16 off={}%",
				String.format("%.3f", meanAbsRgbDiff), String.format("%.3f", maxChannelDiffShare * 100));
	}

	private static Path outDir() {
		return Path.of(System.getProperty("quillcore.autobench.out", "quill-test-report"));
	}

	private void endSegment() {
		BenchHarness.stop();
		BenchMetrics m = BenchHarness.snapshot();
		QuillMeshStats.Snapshot st = QuillMeshStats.snapshot();
		m.vanillaVertexBytes = st.vanillaBytes();
		m.quillVertexBytes = st.builtBytes();
		m.vertexByteReductionPct = st.reductionPct();
		m.notes = m.notes + "; live SOLID bytes (summed compiled sections); quillSections=" + st.quillSections()
				+ " fallbackSections=" + st.fallbackSections() + " rebuildTicks=" + segRebuildTicks;
		statsByLabel.merge(SEGMENTS[segment], st, (x, y) -> new QuillMeshStats.Snapshot(x.quillSections() + y.quillSections(),
				x.fallbackSections() + y.fallbackSections(), x.vanillaBytes() + y.vanillaBytes(), x.builtBytes() + y.builtBytes()));
		report.add(SEGMENTS[segment], m, BenchHarness.frameSamplesCopy());
		QuillCore.LOGGER.info("[quill-bench] {} -> {}", SEGMENTS[segment], BenchHarness.report());
		segAvg.computeIfAbsent(SEGMENTS[segment], k -> new java.util.ArrayList<>()).add(new double[]{
				m.avgFps == null ? Double.NaN : m.avgFps, m.pct1LowFps == null ? Double.NaN : m.pct1LowFps,
				m.pct01LowFps == null ? Double.NaN : m.pct01LowFps});
	}

	private final java.util.Map<String, java.util.List<double[]>> segAvg = new java.util.HashMap<>();

	private double mean(String label, int k) {
		return segAvg.getOrDefault(label, java.util.List.of()).stream().mapToDouble(v -> v[k]).average().orElse(Double.NaN);
	}

	/** M1 gate; returns a one-line summary, also written to gate.json. */
	private String gate() {
		double onAvg = mean("on", 0), offAvg = mean("off", 0), on1 = mean("on", 1), off1 = mean("off", 1), on01 = mean("on", 2), off01 = mean("off", 2);
		QuillMeshStats.Snapshot on = statsByLabel.get("on");
		double bytesPct = on == null ? 0 : on.reductionPct();
		boolean gAvg = onAvg >= offAvg * 0.98, g1 = on1 >= off1 * 0.95, gBytes = bytesPct >= 30.0,
				gImg = meanAbsRgbDiff != null && meanAbsRgbDiff <= 3.0;
		boolean pass = gAvg && g1 && gBytes && gImg;
		String json = String.format(java.util.Locale.ROOT,
				"{\"pass\": %b, \"avgFps\": {\"on\": %.2f, \"off\": %.2f, \"pass\": %b},"
						+ " \"pct1Low\": {\"on\": %.2f, \"off\": %.2f, \"pass\": %b},"
						+ " \"pct01Low\": {\"on\": %.2f, \"off\": %.2f},"
						+ " \"solidBytes\": {\"vanilla\": %d, \"built\": %d, \"reductionPct\": %.2f, \"quillSections\": %d, \"fallbackSections\": %d, \"pass\": %b},"
						+ " \"meshCompare\": {\"meanAbsRgbDiff\": %s, \"channelsOver16Pct\": %.4f, \"compareQuillSections\": %d, \"compareFallbackSections\": %d, \"pass\": %b}}",
				pass, onAvg, offAvg, gAvg, on1, off1, g1, on01, off01,
				on == null ? 0 : on.vanillaBytes(), on == null ? 0 : on.builtBytes(), bytesPct,
				on == null ? 0 : on.quillSections(), on == null ? 0 : on.fallbackSections(), gBytes,
				meanAbsRgbDiff == null ? "null" : String.format(java.util.Locale.ROOT, "%.4f", meanAbsRgbDiff), maxChannelDiffShare * 100,
				compareStats == null ? 0 : compareStats.quillSections(), compareStats == null ? 0 : compareStats.fallbackSections(), gImg);
		try {
			java.nio.file.Files.createDirectories(outDir());
			java.nio.file.Files.writeString(outDir().resolve("gate.json"), json + "\n");
		} catch (Exception e) {
			QuillCore.LOGGER.error("[quill-bench] gate.json write failed", e);
		}
		QuillCore.LOGGER.info("[quill-bench] gate -> {}", json);
		return String.format(java.util.Locale.ROOT, "gate=%s(avg %b, 1%%low %b, bytes %.1f%% %b, img %s %b)", pass ? "PASS" : "FAIL",
				gAvg, g1, bytesPct, gBytes, meanAbsRgbDiff == null ? "n/a" : String.format(java.util.Locale.ROOT, "%.3f", meanAbsRgbDiff), gImg);
	}

	private void finish(Minecraft mc, String error) {
		done = true;
		String gateLine = error == null ? gate() : "gate=n/a";
		QuillConfig.get().masterEnabled = true;
		QuillConfig.get().compactMeshEnabled = false;
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
				+ "; fullscreen=" + mc.getWindow().isFullscreen() + "; segmentSeconds=" + segTicks / 20 + "; seed=20261023; renderDistance=12; vsync=off; cap=unlimited; throttling=disabled; unfocusedTicks=" + unfocusedTicks + "; iconifiedTicks=" + iconifiedTicks + "; " + gateLine);
		try {
			Path out = outDir();
			report.write(out);
			QuillCore.LOGGER.info("[quill-bench] wrote {}", out.toAbsolutePath());
		} catch (Exception e) {
			QuillCore.LOGGER.error("[quill-bench] failed to write report", e);
		}
		mc.stop();
	}
}
