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
	@Unique
	private RenderPass.Draw<GpuBufferSlice[]>[] quillcore$faceDraws;
	@Unique
	private RenderPass.Draw<GpuBufferSlice[]> quillcore$faceSource;
	@Unique
	private int quillcore$faceMask = -1;
	@Unique
	private int[] quillcore$faceGroups;

	@Override
	public RenderPass.Draw<GpuBufferSlice[]> quillcore$draw() {
		return quillcore$draw;
	}

	@Override
	public void quillcore$setDraw(RenderPass.Draw<GpuBufferSlice[]> draw) {
		quillcore$draw = draw;
	}

	@Override
	public RenderPass.Draw<GpuBufferSlice[]>[] quillcore$faceDraws() {
		return quillcore$faceDraws;
	}

	@Override
	public int quillcore$faceMask() {
		return quillcore$faceMask;
	}

	@Override
	public RenderPass.Draw<GpuBufferSlice[]> quillcore$faceSource() {
		return quillcore$faceSource;
	}

	@Override
	public void quillcore$setFaceDraws(RenderPass.Draw<GpuBufferSlice[]>[] draws, int mask, RenderPass.Draw<GpuBufferSlice[]> source) {
		quillcore$faceDraws = draws;
		quillcore$faceSource = source;
		quillcore$faceMask = mask;
	}

	@Override
	public int[] quillcore$faceGroups() {
		return quillcore$faceGroups;
	}

	@Override
	public void quillcore$setFaceGroups(int[] groups) {
		quillcore$faceGroups = groups;
	}
}
