package com.chopstickshq.quillcore.client;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.compat.CompatStatus;
import com.chopstickshq.quillcore.upscale.UpscaleRegistry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Cloth Config screen. Only loaded when Cloth Config is present (ModMenu entrypoint).
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

		ConfigCategory upscale = builder.getOrCreateCategory(Component.literal("Upscale"));
		upscale.addEntry(entry.startTextDescription(Component.literal(CompatStatus.summarize())).build());
		upscale.addEntry(entry.startTextDescription(Component.literal(
				"Backends: " + UpscaleRegistry.summarize() + ". FSR1 is not implemented yet."
		)).build());
		upscale.addEntry(entry.startStrField(Component.literal("Upscale backend id"), c.upscaleBackendId)
				.setDefaultValue("none")
				.setSaveConsumer(v -> QuillConfig.get().upscaleBackendId = v == null || v.isBlank() ? "none" : v.trim())
				.setTooltip(Component.literal("none or fsr. Saved, but no backend is available yet."))
				.build());

		return builder.build();
	}
}
