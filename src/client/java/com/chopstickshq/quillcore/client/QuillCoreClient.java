package com.chopstickshq.quillcore.client;

import com.chopstickshq.quillcore.bench.BenchHarness;
import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class QuillCoreClient implements ClientModInitializer {
	private static long lastFrameNs;

	@Override
	public void onInitializeClient() {
		WorldRenderEvents.END.register(context -> {
			if (!(QuillConfig.get().benchEnabled || BenchHarness.isRunning())) {
				lastFrameNs = 0L;
				return;
			}
			long now = System.nanoTime();
			if (lastFrameNs > 0L) {
				BenchHarness.recordFrameNanos(now - lastFrameNs);
			}
			lastFrameNs = now;
		});
		QuillCore.LOGGER.info("QuillCore client: render cutoffs on when renderEnabled. {}", QuillConfig.get().statusSummary());
	}
}
