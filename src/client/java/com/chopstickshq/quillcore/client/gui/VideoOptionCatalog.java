package com.chopstickshq.quillcore.client.gui;

import com.chopstickshq.quillcore.client.gui.QuillOption.Impact;
import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.settings.StagedSettings;
import com.chopstickshq.quillcore.upscale.fsr.FsrController;
import com.chopstickshq.quillcore.upscale.fsr.FsrQualityPreset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.util.OptionEnum;

/** Builds the option pages and binds each row to its backing value in {@link StagedSettings}. */
final class VideoOptionCatalog {
	private final Minecraft minecraft;
	private final StagedSettings staged;
	private final Runnable openVanilla;
	private final Runnable openFullConfig;

	VideoOptionCatalog(Minecraft minecraft, StagedSettings staged, Runnable openVanilla, Runnable openFullConfig) {
		this.minecraft = minecraft;
		this.staged = staged;
		this.openVanilla = openVanilla;
		this.openFullConfig = openFullConfig;
	}

	List<OptionPage> build() {
		Options o = minecraft.options;
		List<OptionPage> pages = new ArrayList<>();

		List<QuillOption> general = new ArrayList<>();
		general.add(vanillaSlider("renderDistance", o.renderDistance(), 2, 32, "Render distance", Impact.HIGH,
				"How many chunks around you are drawn. The single biggest lever on frame rate and memory.", v -> v + " chunks"));
		general.add(vanillaSlider("simulationDistance", o.simulationDistance(), 5, 32, "Simulation distance", Impact.HIGH,
				"How far away entities and blocks keep ticking in singleplayer. Servers decide this for themselves.", v -> v + " chunks"));
		bind("maxFps", o.framerateLimit());
		general.add(new QuillOption.Slider("maxFps", "Max framerate", "Caps frames per second. Lower caps save power and heat; 'Unlimited' runs as fast as possible.",
				Impact.MEDIUM, 10, 260, 10, v -> v >= 260 ? "Unlimited" : v + " fps"));
		bind("vsync", o.enableVsync());
		general.add(new QuillOption.Toggle("vsync", "VSync", "Syncs frames to the display refresh. Removes tearing, may add a little input latency.", Impact.LOW));
		bind("fullscreen", o.fullscreen());
		general.add(new QuillOption.Toggle("fullscreen", "Fullscreen", "Run the game in exclusive fullscreen.", Impact.NONE));
		general.add(vanillaSlider("guiScale", o.guiScale(), 0, Math.max(1, minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode())),
				"GUI scale", Impact.NONE, "Size of menus and the HUD. 'Auto' picks the largest size that fits.", v -> v == 0 ? "Auto" : v + "x"));
		bindDouble("brightness", o.gamma(), 100.0);
		general.add(new QuillOption.Slider("brightness", "Brightness", "Gamma. Purely visual.", Impact.NONE, 0, 100, 5,
				v -> v == 0 ? "Moody" : v == 100 ? "Bright" : v + "%"));
		bind("bobView", o.bobView());
		general.add(new QuillOption.Toggle("bobView", "View bobbing", "Camera sway while walking.", Impact.NONE));
		general.add(vanillaCycle("inactivityFps", o.inactivityFpsLimit(), "Unfocused framerate", Impact.SAVES,
				"Lowers the frame rate while the window is unfocused or minimized."));
		bindConfig("masterEnabled", () -> QuillConfig.get().masterEnabled, v -> QuillConfig.get().masterEnabled = v);
		general.add(new QuillOption.Toggle("masterEnabled", "QuillCore enabled", "Master switch. Off turns every QuillCore optimization off at once.", Impact.SAVES));
		pages.add(new OptionPage("General", general));

		List<QuillOption> quality = new ArrayList<>();
		quality.add(vanillaCycle("graphics", o.graphicsMode(), "Graphics", Impact.HIGH,
				"Fast skips transparent leaves and some effects. Fabulous adds translucency sorting and costs the most."));
		quality.add(vanillaCycle("clouds", o.cloudStatus(), "Clouds", Impact.MEDIUM, "Cloud style, or none at all."));
		quality.add(vanillaSlider("cloudRange", o.cloudRange(), 2, 128, "Cloud distance", Impact.LOW,
				"How far clouds are drawn (vanilla). QuillCore can cap this further under Performance.", v -> v + " chunks"));
		bind("ao", o.ambientOcclusion());
		quality.add(new QuillOption.Toggle("ao", "Smooth lighting", "Soft shading in block corners.", Impact.LOW));
		bind("shadows", o.entityShadows());
		quality.add(new QuillOption.Toggle("shadows", "Entity shadows", "Round shadows under mobs and items.", Impact.LOW));
		bindDouble("entityDistance", o.entityDistanceScaling(), 4.0);
		quality.add(new QuillOption.Slider("entityDistance", "Entity distance", "Vanilla scale for how far entities stay visible.",
				Impact.MEDIUM, 2, 20, 1, v -> (v * 25) + "%"));
		quality.add(vanillaSlider("biomeBlend", o.biomeBlendRadius(), 0, 7, "Biome blend", Impact.MEDIUM,
				"Smooths grass/water colour between biomes. Higher values slow chunk building.", v -> v == 0 ? "Off" : (v * 2 + 1) + "x" + (v * 2 + 1)));
		quality.add(vanillaSlider("mipmap", o.mipmapLevels(), 0, 4, "Mipmap levels", Impact.LOW,
				"Smooths distant textures. Applying reloads textures.", v -> v == 0 ? "Off" : String.valueOf(v)));
		quality.add(vanillaCycle("chunkUpdates", o.prioritizeChunkUpdates(), "Chunk builder", Impact.MEDIUM,
				"Which chunk rebuilds happen on the main thread. Blocking options reduce visible holes but can stutter."));
		pages.add(new OptionPage("Quality", quality));

		List<QuillOption> perf = new ArrayList<>();
		perf.add(new QuillOption.Note("Quill Render", true, () -> "", ""));
		bindConfig("renderEnabled", () -> QuillConfig.get().renderEnabled, v -> QuillConfig.get().renderEnabled = v);
		perf.add(new QuillOption.Toggle("renderEnabled", "Quill Render", "All client-side render cutoffs and caps below.", Impact.SAVES));
		cutoff(perf, "entityCutoff", "Entity render cutoff", "Skip drawing entities beyond this distance. Camera and your vehicle are exempt.",
				() -> QuillConfig.get().entityRenderCutoffEnabled, v -> QuillConfig.get().entityRenderCutoffEnabled = v,
				() -> QuillConfig.get().entityRenderCutoffBlocks, v -> QuillConfig.get().entityRenderCutoffBlocks = v);
		cutoff(perf, "beCutoff", "Block entity cutoff", "Skip distant signs and skulls. Chests and animated block entities are never skipped.",
				() -> QuillConfig.get().blockEntityRenderCutoffEnabled, v -> QuillConfig.get().blockEntityRenderCutoffEnabled = v,
				() -> QuillConfig.get().blockEntityRenderCutoffBlocks, v -> QuillConfig.get().blockEntityRenderCutoffBlocks = v);
		cutoff(perf, "particleCutoff", "Particle cutoff", "Don't spawn particles beyond this distance from the camera. Visual only.",
				() -> QuillConfig.get().particleRenderCutoffEnabled, v -> QuillConfig.get().particleRenderCutoffEnabled = v,
				() -> QuillConfig.get().particleRenderCutoffBlocks, v -> QuillConfig.get().particleRenderCutoffBlocks = v);
		bindConfig("cloudCap", () -> QuillConfig.get().cloudDistanceCapEnabled, v -> QuillConfig.get().cloudDistanceCapEnabled = v);
		perf.add(new QuillOption.Toggle("cloudCap", "Cloud distance cap", "Caps the vanilla cloud distance further. Off by default.", Impact.SAVES));
		bindConfig("cloudCapChunks", () -> QuillConfig.get().cloudDistanceCapChunks, v -> QuillConfig.get().cloudDistanceCapChunks = v);
		perf.add(new QuillOption.Slider("cloudCapChunks", "  Cap", "Maximum cloud distance while the cap is on.", Impact.SAVES, 1, 128, 1, v -> v + " chunks").requires("cloudCap"));
		bindConfig("weatherCap", () -> QuillConfig.get().weatherRadiusCapEnabled, v -> QuillConfig.get().weatherRadiusCapEnabled = v);
		perf.add(new QuillOption.Toggle("weatherCap", "Weather radius cap", "Caps the rain/snow column radius. Low values show a visible edge.", Impact.SAVES));
		bindConfig("weatherCapBlocks", () -> QuillConfig.get().weatherRadiusCapBlocks, v -> QuillConfig.get().weatherRadiusCapBlocks = v);
		perf.add(new QuillOption.Slider("weatherCapBlocks", "  Radius", "Rain/snow radius while the cap is on.", Impact.SAVES, 1, 10, 1, v -> v + " blocks").requires("weatherCap"));
		perf.add(new QuillOption.Note("Quill Logic", true, () -> "", ""));
		bindConfig("logicEnabled", () -> QuillConfig.get().logicEnabled, v -> QuillConfig.get().logicEnabled = v);
		perf.add(new QuillOption.Toggle("logicEnabled", "Quill Logic", "Server-side tick throttles for idle, far-away mobs and items.", Impact.SAVES));
		logic(perf, "idleAi", "Idle AI throttle", "Skip goal ticks for idle mobs far from players. Sensing and movement still run.",
				() -> QuillConfig.get().idleAiThrottleEnabled, v -> QuillConfig.get().idleAiThrottleEnabled = v);
		logic(perf, "brainAi", "Villager brain throttle", "Skip brain ticks for idle distant villagers — never while trading, panicking or hurt.",
				() -> QuillConfig.get().brainIdleThrottleEnabled, v -> QuillConfig.get().brainIdleThrottleEnabled = v);
		logic(perf, "itemMerge", "Item/XP merge throttle", "Fewer neighbour-merge scans for far items and orbs. Pickup and despawn timing untouched.",
				() -> QuillConfig.get().itemMergeThrottleEnabled, v -> QuillConfig.get().itemMergeThrottleEnabled = v);
		logic(perf, "pathNav", "Path navigation throttle", "Skip navigation ticks for far idle mobs with no active path.",
				() -> QuillConfig.get().pathNavThrottleEnabled, v -> QuillConfig.get().pathNavThrottleEnabled = v);
		pages.add(new OptionPage("Performance", perf));

		List<QuillOption> fsr = new ArrayList<>();
		bindConfig("fsr", () -> "fsr1".equals(QuillConfig.get().upscaleBackendId), v -> QuillConfig.get().upscaleBackendId = v ? "fsr1" : "none");
		fsr.add(new QuillOption.Toggle("fsr", "FSR 1 upscaling", "Renders the world at a lower resolution and upscales it with AMD FSR 1 (EASU + RCAS). The HUD stays native.", Impact.SAVES));
		bindConfig("fsrPreset", () -> FsrQualityPreset.fromId(QuillConfig.get().fsrQualityPreset), v -> QuillConfig.get().fsrQualityPreset = v.name());
		fsr.add(new QuillOption.Cycle<>("fsrPreset", "Quality preset", "Internal render scale. Ultra Quality is sharpest, Ultra Performance is fastest.",
				Impact.SAVES, Arrays.asList(FsrQualityPreset.values()), p -> p.label() + " (" + Math.round(p.renderScale() * 100) + "%)").requires("fsr"));
		bindConfig("fsrSharpness", () -> Math.round(QuillConfig.get().fsrSharpness * 10), v -> QuillConfig.get().fsrSharpness = v / 10f);
		fsr.add(new QuillOption.Slider("fsrSharpness", "Sharpness", "RCAS sharpening. 0 is the sharpest, 2 the softest.", Impact.LOW, 0, 20, 1,
				v -> String.format("%.1f", v / 10f)).requires("fsr"));
		bindConfig("fsrScale", () -> Math.round(QuillConfig.get().fsrRenderScaleOverride * 100), v -> QuillConfig.get().fsrRenderScaleOverride = v / 100f);
		fsr.add(new QuillOption.Slider("fsrScale", "Render scale override", "Manual internal scale. 'Preset' follows the quality preset; 100% disables upscaling.",
				Impact.SAVES, 0, 100, 5, v -> v == 0 ? "Preset" : Math.max(25, v) + "%").requires("fsr"));
		fsr.add(new QuillOption.Note("Status", false, FsrController::runtimeStatus, "Live state of the upscaler."));
		pages.add(new OptionPage("Upscaling (FSR)", fsr));

		List<QuillOption> adv = new ArrayList<>();
		bindConfig("bench", () -> QuillConfig.get().benchEnabled, v -> QuillConfig.get().benchEnabled = v);
		adv.add(new QuillOption.Toggle("bench", "Bench sampling", "Sample server ticks and client frames for /quillcore bench report.", Impact.LOW));
		adv.add(new QuillOption.Action("Vanilla video settings", "Open Minecraft's original Video Settings screen.", "Open", openVanilla));
		if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
			adv.add(new QuillOption.Action("All QuillCore options", "Every tunable, including throttle radii and skip ticks (Cloth Config).", "Open",
					openFullConfig));
		}
		adv.add(new QuillOption.Note("Config file", false, () -> "config/quillcore.json", "Where these settings are stored."));
		pages.add(new OptionPage("Advanced", adv));
		return pages;
	}

	private void cutoff(List<QuillOption> list, String id, String label, String desc,
			Supplier<Boolean> enabled, Consumer<Boolean> setEnabled, Supplier<Double> blocks, Consumer<Double> setBlocks) {
		bindConfig(id, enabled, setEnabled);
		list.add(new QuillOption.Toggle(id, label, desc, Impact.SAVES));
		bindConfig(id + "Blocks", () -> (int) Math.round(blocks.get()), v -> setBlocks.accept((double) (int) v));
		list.add(new QuillOption.Slider(id + "Blocks", "  Distance", desc, Impact.SAVES, 16, 512, 8, v -> v + " blocks").requires(id));
	}

	private void logic(List<QuillOption> list, String id, String label, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
		bindConfig(id, get, set);
		list.add(new QuillOption.Toggle(id, label, desc, Impact.SAVES).requires("logicEnabled"));
	}

	private <T> void bindConfig(String id, Supplier<T> get, Consumer<T> set) {
		staged.bind(id, get, set);
	}

	private <T> void bind(String id, OptionInstance<T> option) {
		staged.bind(id, option::get, option::set);
	}

	private void bindDouble(String id, OptionInstance<Double> option, double scale) {
		staged.bind(id, () -> (int) Math.round(option.get() * scale), (Integer v) -> option.set(v / scale));
	}

	private QuillOption vanillaSlider(String id, OptionInstance<Integer> option, int min, int max, String label, Impact impact,
			String desc, IntFunction<String> fmt) {
		if (option.values() instanceof OptionInstance.IntRange r) {
			min = r.minInclusive();
			max = r.maxInclusive();
		}
		bind(id, option);
		return new QuillOption.Slider(id, label, desc, impact, min, max, 1, fmt);
	}

	private <T extends Enum<T> & OptionEnum> QuillOption vanillaCycle(String id, OptionInstance<T> option, String label, Impact impact, String desc) {
		bind(id, option);
		List<T> values = Arrays.asList(option.get().getDeclaringClass().getEnumConstants());
		return new QuillOption.Cycle<>(id, label, desc, impact, values, v -> v.getCaption().getString());
	}
}
