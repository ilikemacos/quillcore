package com.chopstickshq.quillcore.common;

import com.chopstickshq.quillcore.upscale.UpscaleRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QuillCore implements ModInitializer {
	public static final String MOD_ID = "quillcore";
	public static final Logger LOGGER = LoggerFactory.getLogger("QuillCore");

	@Override
	public void onInitialize() {
		QuillConfig.load();
		UpscaleRegistry.bootstrap();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> QuillCommands.register(dispatcher));
		LOGGER.info("QuillCore 0.1.0 — One core. Smoother worlds. {}", QuillConfig.get().statusSummary());
	}
}
