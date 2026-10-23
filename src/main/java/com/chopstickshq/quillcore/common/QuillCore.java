package com.chopstickshq.quillcore.common;

import com.chopstickshq.quillcore.bench.BenchHarness;
import com.chopstickshq.quillcore.upscale.UpscaleRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QuillCore implements ModInitializer {
	public static final String MOD_ID = "quillcore";
	public static final Logger LOGGER = LoggerFactory.getLogger("QuillCore");

	private static long tickStartNs;

	@Override
	public void onInitialize() {
		QuillConfig.load();
		UpscaleRegistry.bootstrap();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> QuillCommands.register(dispatcher));
		ServerTickEvents.START_SERVER_TICK.register(server -> tickStartNs = System.nanoTime());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (QuillConfig.get().benchEnabled || BenchHarness.isRunning()) {
				BenchHarness.recordTickNanos(System.nanoTime() - tickStartNs);
			}
		});
		LOGGER.info("QuillCore 0.4.0 — One core. Smoother worlds. {}", QuillConfig.get().statusSummary());
	}
}
