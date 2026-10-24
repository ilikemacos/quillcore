package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.ramcache.MeshCacheHook;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererRamCacheMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void quillcore$frame(CallbackInfo ci) {
		MeshCacheHook.endFrame();
	}
}
