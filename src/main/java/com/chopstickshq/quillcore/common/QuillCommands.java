package com.chopstickshq.quillcore.common;

import com.chopstickshq.quillcore.bench.BenchHarness;
import com.chopstickshq.quillcore.compat.CompatStatus;
import com.chopstickshq.quillcore.logic.IdleAiThrottleState;
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
							return 1;
						}))
				.then(Commands.literal("reload")
						.executes(ctx -> {
							QuillConfig.load();
							IdleAiThrottleState.clear();
							ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] Reloaded config: " + QuillConfig.get().statusSummary()), true);
							return 1;
						}))
				.then(Commands.literal("set")
						.then(Commands.argument("key", StringArgumentType.word())
								.then(Commands.argument("value", StringArgumentType.greedyString())
										.executes(ctx -> set(ctx.getSource(),
												StringArgumentType.getString(ctx, "key"),
												StringArgumentType.getString(ctx, "value"))))))
				.then(Commands.literal("bench")
						.then(Commands.literal("start")
								.executes(ctx -> {
									BenchHarness.start("manual");
									ctx.getSource().sendSuccess(() -> Component.literal("[QuillCore] Bench started (samples only; no fabricated metrics)."), false);
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
				case "benchEnabled" -> c.benchEnabled = parseBool(value);
				default -> {
					source.sendFailure(Component.literal("Unknown key: " + key
							+ " (masterEnabled|renderEnabled|logicEnabled|idleAiThrottleEnabled|idleAiThrottleMaxSkipTicks|idleAiThrottlePlayerRadiusBlocks|benchEnabled)"));
					return 0;
				}
			}
		} catch (RuntimeException e) {
			source.sendFailure(Component.literal("Bad value for " + key + ": " + e.getMessage()));
			return 0;
		}
		QuillConfig.save();
		source.sendSuccess(() -> Component.literal("[QuillCore] Set " + key + " — " + c.statusSummary()), true);
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
