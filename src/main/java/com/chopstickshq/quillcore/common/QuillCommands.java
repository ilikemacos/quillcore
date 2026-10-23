package com.chopstickshq.quillcore.common;

import com.chopstickshq.quillcore.bench.BenchHarness;
import com.chopstickshq.quillcore.compat.CompatStatus;
import com.chopstickshq.quillcore.framefx.FrameFxLauncher;
import com.chopstickshq.quillcore.logic.BrainIdleThrottleState;
import com.chopstickshq.quillcore.logic.IdleAiThrottleState;
import com.chopstickshq.quillcore.logic.ItemMergeThrottleState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class QuillCommands {
	private QuillCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("quillcore")
				.requires(src -> src.hasPermission(2))
				.then(Commands.literal("status")
						.executes(ctx -> {
							QuillConfig c = QuillConfig.get();
							ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] " + c.statusSummary()), false);
							ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] " + CompatStatus.summarize()), false);
							ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] " + FrameFxLauncher.probeSummary(c.frameFxExecutablePath)), false);
							return 1;
						}))
				.then(Commands.literal("reload")
						.executes(ctx -> {
							QuillConfig.load();
							IdleAiThrottleState.clear();
							BrainIdleThrottleState.clear();
							ItemMergeThrottleState.clear();
							ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] Reloaded config: " + QuillConfig.get().statusSummary()), true);
							return 1;
						}))
				.then(Commands.literal("reset")
						.executes(ctx -> {
							QuillConfig.resetToDefaults();
							IdleAiThrottleState.clear();
							BrainIdleThrottleState.clear();
							ItemMergeThrottleState.clear();
							ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] Reset to defaults: " + QuillConfig.get().statusSummary()), true);
							return 1;
						}))
				.then(Commands.literal("set")
						.then(Commands.argument("key", StringArgumentType.word())
								.then(Commands.argument("value", StringArgumentType.greedyString())
										.executes(ctx -> set(ctx.getSource(),
												StringArgumentType.getString(ctx, "key"),
												StringArgumentType.getString(ctx, "value"))))))
				.then(Commands.literal("framefx")
						.then(Commands.literal("probe")
								.executes(ctx -> {
									String s = FrameFxLauncher.probeSummary(QuillConfig.get().frameFxExecutablePath);
									ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] " + s), false);
									return 1;
								}))
						.then(Commands.literal("launch")
								.executes(ctx -> {
									var err = FrameFxLauncher.launch(QuillConfig.get().frameFxExecutablePath);
									if (err.isPresent()) {
										ctx.getSource().sendFailure(Component.literal("[QuillCore] " + err.get()));
										return 0;
									}
									ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] Launched Universal-FrameFX externally (no CSR flags; not in-process)."), false);
									return 1;
								})))
				.then(Commands.literal("bench")
						.then(Commands.literal("start")
								.executes(ctx -> {
									QuillConfig c = QuillConfig.get();
									c.benchEnabled = true;
									QuillConfig.save();
									BenchHarness.start("manual");
									ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] Bench started."), false);
									return 1;
								}))
						.then(Commands.literal("stop")
								.executes(ctx -> {
									BenchHarness.stop();
									ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] Bench stopped."), false);
									return 1;
								}))
						.then(Commands.literal("report")
								.executes(ctx -> {
									String report = BenchHarness.report();
									ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] " + report), false);
									return 1;
								}))));
	}

	private static int set(CommandSourceStack source, String key, String value) {
		QuillConfig c = QuillConfig.get();
		try {
			switch (key) {
				case "masterEnabled" -> c.masterEnabled = parseBool(value);
				case "renderEnabled" -> c.renderEnabled = parseBool(value);
				case "logicEnabled" -> c.logicEnabled = parseBool(value);
				case "idleAiThrottleEnabled" -> c.idleAiThrottleEnabled = parseBool(value);
				case "idleAiThrottleMaxSkipTicks" -> c.idleAiThrottleMaxSkipTicks = Integer.parseInt(value.trim());
				case "idleAiThrottlePlayerRadiusBlocks" -> c.idleAiThrottlePlayerRadiusBlocks = Double.parseDouble(value.trim());
				case "brainIdleThrottleEnabled" -> c.brainIdleThrottleEnabled = parseBool(value);
				case "brainIdleThrottleMaxSkipTicks" -> c.brainIdleThrottleMaxSkipTicks = Integer.parseInt(value.trim());
				case "brainIdleThrottlePlayerRadiusBlocks" -> c.brainIdleThrottlePlayerRadiusBlocks = Double.parseDouble(value.trim());
				case "entityRenderCutoffEnabled" -> c.entityRenderCutoffEnabled = parseBool(value);
				case "entityRenderCutoffBlocks" -> c.entityRenderCutoffBlocks = Double.parseDouble(value.trim());
				case "blockEntityRenderCutoffEnabled" -> c.blockEntityRenderCutoffEnabled = parseBool(value);
				case "blockEntityRenderCutoffBlocks" -> c.blockEntityRenderCutoffBlocks = Double.parseDouble(value.trim());
				case "particleRenderCutoffEnabled" -> c.particleRenderCutoffEnabled = parseBool(value);
				case "particleRenderCutoffBlocks" -> c.particleRenderCutoffBlocks = Double.parseDouble(value.trim());
				case "itemMergeThrottleEnabled" -> c.itemMergeThrottleEnabled = parseBool(value);
				case "itemMergeThrottleMaxSkipTicks" -> c.itemMergeThrottleMaxSkipTicks = Integer.parseInt(value.trim());
				case "itemMergeThrottlePlayerRadiusBlocks" -> c.itemMergeThrottlePlayerRadiusBlocks = Double.parseDouble(value.trim());
				case "frameFxExecutablePath" -> c.frameFxExecutablePath = value.trim();
				case "upscaleBackendId" -> c.upscaleBackendId = value.trim();
				case "frameGenEnabled" -> {
					c.frameGenEnabled = false;
					source.sendSuccess(() -> Component.literal("[QuillCore] frameGenEnabled is unused and stays false."), false);
				}
				case "benchEnabled" -> c.benchEnabled = parseBool(value);
				default -> {
					source.sendFailure(Component.literal("Unknown key: " + key
							+ " (masterEnabled|renderEnabled|logicEnabled|idleAi*|brainIdle*|entityRenderCutoff*|blockEntityRenderCutoff*|particleRenderCutoff*|itemMergeThrottle*|frameFxExecutablePath|upscaleBackendId|frameGenEnabled|benchEnabled)"));
					return 0;
				}
			}
		} catch (RuntimeException e) {
			source.sendFailure(Component.literal("Bad value for " + key + ": " + e.getMessage()));
			return 0;
		}
		QuillConfig.save();
		source.sendSuccess(() -> Component.literal("[QuillCore] Set " + key + ": " + c.statusSummary()), true);
		return 1;
	}

	private static boolean parseBool(String value) {
		String v = value.trim().toLowerCase();
		return switch (v) {
			case "true", "1", "on", "yes" -> true;
			case "false", "0", "off", "no" -> false;
			default -> throw new IllegalArgumentException("expected boolean, got: " + value);
		};
	}
}
