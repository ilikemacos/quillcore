package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.upscale.fsr.FsrController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shrinks the main RenderTarget before {@code renderLevel}, then runs FSR1 EASU+RCAS
 * after world/post and before GUI so HUD stays native resolution.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererFsrMixin {
	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"
			)
	)
	private void quillcore$fsrBeforeLevel(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		FsrController.beforeWorldRender();
	}

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"
			)
	)
	private void quillcore$fsrAfterWorldBeforeGui(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		FsrController.afterWorldBeforeGui();
	}
}
