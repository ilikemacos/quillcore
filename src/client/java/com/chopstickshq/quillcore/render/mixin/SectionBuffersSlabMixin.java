package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.region.RegionSlabs;
import com.chopstickshq.quillcore.render.region.SlabBacked;
import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A slab backed section hands out the shared region buffer and gives its range back on close. */
@Mixin(SectionBuffers.class)
public abstract class SectionBuffersSlabMixin implements SlabBacked {
	@Shadow
	private GpuBuffer indexBuffer;

	@Unique
	private RegionSlabs.Range quillcore$range;

	@Override
	public RegionSlabs.Range quillcore$range() {
		return quillcore$range;
	}

	@Override
	public void quillcore$setRange(RegionSlabs.Range range) {
		quillcore$range = range;
	}

	@Inject(method = "getVertexBuffer", at = @At("HEAD"), cancellable = true)
	private void quillcore$slabBuffer(CallbackInfoReturnable<GpuBuffer> cir) {
		if (quillcore$range != null) cir.setReturnValue(quillcore$range.slab().buffer());
	}

	@Inject(method = "close", at = @At("HEAD"), cancellable = true)
	private void quillcore$freeRange(CallbackInfo ci) {
		if (quillcore$range == null) return;
		RegionSlabs.free(quillcore$range);
		quillcore$range = null;
		if (indexBuffer != null) indexBuffer.close();
		ci.cancel();
	}
}
