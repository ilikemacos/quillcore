package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.render.CloudDistanceCap;
import net.minecraft.client.renderer.CloudRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Optionally caps {@code min(cloudRange, 128)} before cloud mesh build.
 * Default off — vanilla Cloud Distance remains the primary control.
 */
@Mixin(CloudRenderer.class)
public abstract class CloudDistanceCapMixin {
	@WrapOperation(
			method = "render",
			at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I")
	)
	private int quillcore$capCloudChunks(int a, int b, Operation<Integer> original) {
		int vanilla = original.call(a, b);
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
