package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.csr.client.CsrController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CSR Frame Gen: the finished frame (world + HUD) is in the main target and already blitted to
 * the back buffer; present the interpolated frame first, then let vanilla swap the real one.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftCsrFgMixin {
	@Inject(
			method = "runTick",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V"
			)
	)
	private void quillcore$csrBeforePresent(boolean advanceGameTime, CallbackInfo ci) {
		CsrController.beforePresent();
	}
}
