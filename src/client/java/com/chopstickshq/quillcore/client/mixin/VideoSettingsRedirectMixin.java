package com.chopstickshq.quillcore.client.mixin;

import com.chopstickshq.quillcore.client.gui.QuillVideoSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Options → Video Settings opens the QuillCore screen; its "Vanilla settings…" button passes through once. */
@Mixin(Minecraft.class)
public abstract class VideoSettingsRedirectMixin {
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void quillcore$redirectVideoSettings(Screen screen, CallbackInfo ci) {
		if (screen instanceof VideoSettingsScreen vanilla && !QuillVideoSettingsScreen.consumeVanillaPassThrough()) {
			ci.cancel();
			((Minecraft) (Object) this).setScreen(new QuillVideoSettingsScreen(((OptionsSubScreenAccessor) vanilla).quillcore$lastScreen()));
		}
	}
}
