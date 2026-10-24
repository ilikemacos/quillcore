package com.chopstickshq.quillcore.client;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.compat.CompatStatus;
import com.chopstickshq.quillcore.framefx.FrameFxLauncher;
import com.chopstickshq.quillcore.upscale.UpscaleRegistry;
import com.chopstickshq.quillcore.upscale.fsr.FsrController;
import com.chopstickshq.quillcore.upscale.fsr.FsrQualityPreset;
import java.util.Arrays;
import java.util.List;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Cloth Config screen with every tunable. Only loaded when Cloth Config is present — opened from
 * the Advanced tab of {@code QuillVideoSettingsScreen}.
 * Every control maps to a real QuillConfig field / code path.
 */
public final class QuillCoreConfigScreen {
	private QuillCoreConfigScreen() {}

	public static Screen create(Screen parent) {
		QuillConfig c = QuillConfig.get();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("QuillCore"))
				.setSavingRunnable(QuillConfig::save);

		ConfigEntryBuilder entry = builder.entryBuilder();

		ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
		general.addEntry(entry.startBooleanToggle(Component.literal("Master enabled"), c.masterEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> QuillConfig.get().masterEnabled = v)
				.setTooltip(Component.literal("Disables all QuillCore optimizations when off."))
				.build());
		general.addEntry(entry.startBooleanToggle(Component.literal("Quill Render"), c.renderEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> QuillConfig.get().renderEnabled = v)
				.build());
		general.addEntry(entry.startBooleanToggle(Component.literal("Compact chunk meshes (experimental)"), c.compactMeshEnabled)
				.setDefaultValue(false)
				.setSaveConsumer(v -> {
					if (QuillConfig.get().compactMeshEnabled != v) {
						QuillConfig.get().compactMeshEnabled = v;
						net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
						if (mc.level != null) mc.levelRenderer.allChanged();
					}
				})
				.setTooltip(Component.literal("Re-encode solid terrain into a 20-byte vertex format (vanilla: 32). Sections that don't fit stay vanilla."))
				.build());
		general.addEntry(entry.startBooleanToggle(Component.literal("Quill Logic"), c.logicEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> QuillConfig.get().logicEnabled = v)
				.build());
		general.addEntry(entry.startBooleanToggle(Component.literal("Bench sampling"), c.benchEnabled)
				.setDefaultValue(false)
				.setSaveConsumer(v -> QuillConfig.get().benchEnabled = v)
				.setTooltip(Component.literal("When on, server ticks and client frames are sampled for /quillcore bench report."))
				.build());

		ConfigCategory logic = builder.getOrCreateCategory(Component.literal("Quill Logic"));
		logic.addEntry(entry.startBooleanToggle(Component.literal("Idle AI throttle"), c.idleAiThrottleEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> QuillConfig.get().idleAiThrottleEnabled = v)
				.setTooltip(Component.literal("Skip GoalSelector ticks for idle distant mobs (sensing/navigation still run)."))
				.build());
		logic.addEntry(entry.startIntField(Component.literal("Idle AI max skip ticks"), c.idleAiThrottleMaxSkipTicks)
				.setDefaultValue(2).setMin(0).setMax(10)
				.setSaveConsumer(v -> QuillConfig.get().idleAiThrottleMaxSkipTicks = v)
				.build());
		logic.addEntry(entry.startDoubleField(Component.literal("Idle AI player radius"), c.idleAiThrottlePlayerRadiusBlocks)
				.setDefaultValue(32.0).setMin(0.0).setMax(256.0)
				.setSaveConsumer(v -> QuillConfig.get().idleAiThrottlePlayerRadiusBlocks = v)
				.build());
		logic.addEntry(entry.startBooleanToggle(Component.literal("Villager brain throttle"), c.brainIdleThrottleEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> QuillConfig.get().brainIdleThrottleEnabled = v)
				.setTooltip(Component.literal("Skip Brain.tick for idle distant villagers (never while trading/panicking/hurt)."))
				.build());
		logic.addEntry(entry.startIntField(Component.literal("Brain max skip ticks"), c.brainIdleThrottleMaxSkipTicks)
				.setDefaultValue(2).setMin(0).setMax(10)
				.setSaveConsumer(v -> QuillConfig.get().brainIdleThrottleMaxSkipTicks = v)
				.build());
		logic.addEntry(entry.startDoubleField(Component.literal("Brain player radius"), c.brainIdleThrottlePlayerRadiusBlocks)
				.setDefaultValue(32.0).setMin(0.0).setMax(256.0)
				.setSaveConsumer(v -> QuillConfig.get().brainIdleThrottlePlayerRadiusBlocks = v)
				.build());
		logic.addEntry(entry.startBooleanToggle(Component.literal("Item/XP merge throttle"), c.itemMergeThrottleEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> QuillConfig.get().itemMergeThrottleEnabled = v)
				.setTooltip(Component.literal("Throttle ItemEntity/ExperienceOrb neighbour-merge scans when far from players. Pickup/age untouched."))
				.build());
		logic.addEntry(entry.startIntField(Component.literal("Item merge max skip ticks"), c.itemMergeThrottleMaxSkipTicks)
				.setDefaultValue(2).setMin(0).setMax(10)
				.setSaveConsumer(v -> QuillConfig.get().itemMergeThrottleMaxSkipTicks = v)
				.build());
		logic.addEntry(entry.startDoubleField(Component.literal("Item merge player radius"), c.itemMergeThrottlePlayerRadiusBlocks)
				.setDefaultValue(32.0).setMin(0.0).setMax(256.0)
				.setSaveConsumer(v -> QuillConfig.get().itemMergeThrottlePlayerRadiusBlocks = v)
				.build());
		logic.addEntry(entry.startBooleanToggle(Component.literal("Path navigation throttle"), c.pathNavThrottleEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> QuillConfig.get().pathNavThrottleEnabled = v)
				.setTooltip(Component.literal("Skip PathNavigation.tick for far idle mobs with no active path."))
				.build());
		logic.addEntry(entry.startIntField(Component.literal("Path nav max skip ticks"), c.pathNavThrottleMaxSkipTicks)
				.setDefaultValue(2).setMin(0).setMax(10)
				.setSaveConsumer(v -> QuillConfig.get().pathNavThrottleMaxSkipTicks = v)
				.build());
		logic.addEntry(entry.startDoubleField(Component.literal("Path nav player radius"), c.pathNavThrottlePlayerRadiusBlocks)
				.setDefaultValue(32.0).setMin(0.0).setMax(256.0)
				.setSaveConsumer(v -> QuillConfig.get().pathNavThrottlePlayerRadiusBlocks = v)
				.build());

		ConfigCategory render = builder.getOrCreateCategory(Component.literal("Quill Render"));
		render.addEntry(entry.startBooleanToggle(Component.literal("Entity render cutoff"), c.entityRenderCutoffEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> QuillConfig.get().entityRenderCutoffEnabled = v)
				.setTooltip(Component.literal("Skip rendering entities beyond the cutoff (camera/vehicle exempt). Default 128 blocks."))
				.build());
		render.addEntry(entry.startDoubleField(Component.literal("Entity cutoff (blocks)"), c.entityRenderCutoffBlocks)
				.setDefaultValue(128.0).setMin(0.0).setMax(512.0)
				.setSaveConsumer(v -> QuillConfig.get().entityRenderCutoffBlocks = v)
				.build());
		render.addEntry(entry.startBooleanToggle(Component.literal("Block-entity render cutoff"), c.blockEntityRenderCutoffEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> QuillConfig.get().blockEntityRenderCutoffEnabled = v)
				.setTooltip(Component.literal("Skip distant signs/skulls only. Chests and animated BEs are never skipped."))
				.build());
		render.addEntry(entry.startDoubleField(Component.literal("Block-entity cutoff (blocks)"), c.blockEntityRenderCutoffBlocks)
				.setDefaultValue(96.0).setMin(0.0).setMax(512.0)
				.setSaveConsumer(v -> QuillConfig.get().blockEntityRenderCutoffBlocks = v)
				.build());
		render.addEntry(entry.startBooleanToggle(Component.literal("Particle spawn cutoff"), c.particleRenderCutoffEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(v -> QuillConfig.get().particleRenderCutoffEnabled = v)
				.setTooltip(Component.literal("Skip creating particles beyond the cutoff from the camera. Client-visual only. Default 64 blocks."))
				.build());
		render.addEntry(entry.startDoubleField(Component.literal("Particle cutoff (blocks)"), c.particleRenderCutoffBlocks)
				.setDefaultValue(64.0).setMin(0.0).setMax(512.0)
				.setSaveConsumer(v -> QuillConfig.get().particleRenderCutoffBlocks = v)
				.build());
		render.addEntry(entry.startBooleanToggle(Component.literal("Cloud distance cap (opt-in)"), c.cloudDistanceCapEnabled)
				.setDefaultValue(false)
				.setSaveConsumer(v -> QuillConfig.get().cloudDistanceCapEnabled = v)
				.setTooltip(Component.literal("Further cap vanilla Cloud Distance (chunks). Default off."))
				.build());
		render.addEntry(entry.startIntField(Component.literal("Cloud distance cap (chunks)"), c.cloudDistanceCapChunks)
				.setDefaultValue(16).setMin(1).setMax(128)
				.setSaveConsumer(v -> QuillConfig.get().cloudDistanceCapChunks = v)
				.build());
		render.addEntry(entry.startBooleanToggle(Component.literal("Weather radius cap (opt-in)"), c.weatherRadiusCapEnabled)
				.setDefaultValue(false)
				.setSaveConsumer(v -> QuillConfig.get().weatherRadiusCapEnabled = v)
				.setTooltip(Component.literal("Cap rain/snow column radius. Default off — visible edge if too low."))
				.build());
		render.addEntry(entry.startIntField(Component.literal("Weather radius cap (blocks)"), c.weatherRadiusCapBlocks)
				.setDefaultValue(6).setMin(1).setMax(10)
				.setSaveConsumer(v -> QuillConfig.get().weatherRadiusCapBlocks = v)
				.build());

		ConfigCategory upscale = builder.getOrCreateCategory(Component.literal("Upscale / Frame gen"));
		upscale.addEntry(entry.startTextDescription(Component.literal(CompatStatus.summarize())).build());
		upscale.addEntry(entry.startTextDescription(Component.literal(
				"Backends: " + UpscaleRegistry.summarize()
						+ ". Runtime: " + FsrController.runtimeStatus()
						+ ". FSR1 = real EASU+RCAS (AMD MIT). CSR excluded."
		)).build());
		List<String> backendIds = List.of("none", "fsr1");
		upscale.addEntry(entry.startStringDropdownMenu(Component.literal("Upscale backend"), c.upscaleBackendId)
				.setDefaultValue("none")
				.setSelections(backendIds)
				.setSaveConsumer(v -> QuillConfig.get().upscaleBackendId = v == null || v.isBlank() ? "none" : v.trim().toLowerCase())
				.setTooltip(Component.literal("fsr1 runs AMD FSR1 spatial. Never labels bilinear as FSR."))
				.build());
		List<String> presets = Arrays.stream(FsrQualityPreset.values()).map(Enum::name).toList();
		upscale.addEntry(entry.startStringDropdownMenu(Component.literal("FSR quality preset"), c.fsrQualityPreset)
				.setDefaultValue("QUALITY")
				.setSelections(presets)
				.setSaveConsumer(v -> QuillConfig.get().fsrQualityPreset = v == null ? "QUALITY" : v)
				.setTooltip(Component.literal("AMD FSR1 quality modes → internal render scale. HUD stays native."))
				.build());
		upscale.addEntry(entry.startFloatField(Component.literal("FSR RCAS sharpness"), c.fsrSharpness)
				.setDefaultValue(0.2f).setMin(0.0f).setMax(2.0f)
				.setSaveConsumer(v -> QuillConfig.get().fsrSharpness = v)
				.setTooltip(Component.literal("0 = max sharpening, 2 = softer. AMD RCAS control."))
				.build());
		upscale.addEntry(entry.startFloatField(Component.literal("FSR render scale override"), c.fsrRenderScaleOverride)
				.setDefaultValue(0.0f).setMin(0.0f).setMax(1.0f)
				.setSaveConsumer(v -> QuillConfig.get().fsrRenderScaleOverride = v)
				.setTooltip(Component.literal("0 = use preset. Otherwise 0.25–1.0 internal scale (≥1 skips FSR1)."))
				.build());
		upscale.addEntry(entry.startBooleanToggle(Component.literal("FSR3 frame generation"), c.fsrFrameGenEnabled)
				.setDefaultValue(false)
				.setSaveConsumer(v -> QuillConfig.get().fsrFrameGenEnabled = v)
				.setTooltip(Component.literal("Stored but ignored — no FG backend. Off by default."))
				.build());
		upscale.addEntry(entry.startBooleanToggle(Component.literal("Frame generation (legacy)"), c.frameGenEnabled)
				.setDefaultValue(false)
				.setSaveConsumer(v -> QuillConfig.get().frameGenEnabled = false)
				.setTooltip(Component.literal("Forced off — no working frame-gen backend."))
				.build());

		ConfigCategory framefx = builder.getOrCreateCategory(Component.literal("FrameFX launcher"));
		framefx.addEntry(entry.startTextDescription(Component.literal(
				FrameFxLauncher.probeSummary(c.frameFxExecutablePath)
						+ " — external Windows overlay only; never in-process. Use /quillcore framefx launch. CSR flags never forwarded."
		)).build());
		framefx.addEntry(entry.startStrField(Component.literal("FrameFX executable path"), c.frameFxExecutablePath)
				.setDefaultValue("")
				.setSaveConsumer(v -> QuillConfig.get().frameFxExecutablePath = v == null ? "" : v.trim())
				.setTooltip(Component.literal("Absolute path to Universal-FrameFX.exe. Empty = PATH probe on Windows."))
				.build());

		return builder.build();
	}
}
