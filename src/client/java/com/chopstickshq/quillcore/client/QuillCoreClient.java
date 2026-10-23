package com.chopstickshq.quillcore.client;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import net.fabricmc.api.ClientModInitializer;

public final class QuillCoreClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		QuillCore.LOGGER.info("QuillCore client init — render module not implemented in 0.1.0; config={}", QuillConfig.get().statusSummary());
	}
}
