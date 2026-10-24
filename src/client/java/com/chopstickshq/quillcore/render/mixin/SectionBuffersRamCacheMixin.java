package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.ramcache.MeshCacheHook;
import com.chopstickshq.quillcore.render.ramcache.RamCached;
import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** an evicted section already closed its vertex buffer, so close only drops the index buffer. */
@Mixin(SectionBuffers.class)
public abstract class SectionBuffersRamCacheMixin implements RamCached {
	@Shadow
	private GpuBuffer indexBuffer;

	@Unique
	private final long quillcore$id = MeshCacheHook.newId();

	@Override
	public long quillcore$cacheId() {
		return quillcore$id;
	}

	@Inject(method = "close", at = @At("HEAD"), cancellable = true)
	private void quillcore$untrack(CallbackInfo ci) {
		boolean gone = MeshCacheHook.evicted(quillcore$id);
		MeshCacheHook.untrack(quillcore$id);
		if (!gone) return;
		if (indexBuffer != null) indexBuffer.close();
		ci.cancel();
	}
}
