package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.render.CloudDistanceCap;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Optionally caps {@code min(cloudRange, 128)} before cloud mesh build.
 * Off by default. Vanilla Cloud Distance still applies.
 */
@Mixin(CloudRenderer.class)
public abstract class CloudDistanceCapMixin {
	@Redirect(
			method = "render",
			at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I")
	)
	private int quillcore$capCloudChunks(int a, int b) {
		int vanilla = Math.min(a, b);
		QuillConfig cfg = QuillConfig.get();
		return CloudDistanceCap.applyChunkCap(
				cfg.masterEnabled,
				cfg.renderEnabled,
				cfg.cloudDistanceCapEnabled,
				cfg.cloudDistanceCapChunks,
				vanilla
		);
	}
}
