package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.prep.DrawCached;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionBuffers.class)
public abstract class SectionBuffersDrawCacheMixin implements DrawCached {
	@Unique
	private RenderPass.Draw<GpuBufferSlice[]> quillcore$draw;

	@Override
	public RenderPass.Draw<GpuBufferSlice[]> quillcore$draw() {
		return quillcore$draw;
	}

	@Override
	public void quillcore$setDraw(RenderPass.Draw<GpuBufferSlice[]> draw) {
		quillcore$draw = draw;
	}
}
