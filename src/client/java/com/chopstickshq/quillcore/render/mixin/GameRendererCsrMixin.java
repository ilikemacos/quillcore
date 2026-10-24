package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.csr.client.CsrController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CSR hooks at the same points as {@link GameRendererFsrMixin}: shrink the main target before
 * {@code renderLevel}, upscale after world/post and before GUI so the HUD stays native.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererCsrMixin {
	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"
			)
	)
	private void quillcore$csrBeforeLevel(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		CsrController.beforeWorldRender();
	}

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"
			)
	)
	private void quillcore$csrAfterWorldBeforeGui(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		CsrController.afterWorldBeforeGui();
	}
}
