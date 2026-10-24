package com.chopstickshq.quillcore.client;

import com.chopstickshq.quillcore.bench.BenchHarness;
import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.csr.client.CsrController;
import com.chopstickshq.quillcore.upscale.fsr.FsrController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class QuillCoreClient implements ClientModInitializer {
	private static long lastFrameNs;

	@Override
	public void onInitializeClient() {
		FsrController.bootstrap();
		CsrController.bootstrap();
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
		QuillCore.LOGGER.info(
				"QuillCore client: FSR1 pipelines ready, {}. CSR: {}. {}",
				FsrController.runtimeStatus(),
				CsrController.runtimeStatus(),
				QuillConfig.get().statusSummary()
		);
	}
}
