package com.chopstickshq.quillcore.render.prep;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;

/**
 * Duck on {@code SectionBuffers}: the last draws built for it, reused while every field they were built from still
 * matches, and, for compact SOLID buffers, the quad count of each face group (+X,-X,+Y,-Y,+Z,-Z) in buffer order.
 */
public interface DrawCached {
	RenderPass.Draw<GpuBufferSlice[]> quillcore$draw();

	void quillcore$setDraw(RenderPass.Draw<GpuBufferSlice[]> draw);

	/** Draws for {@link #quillcore$faceMask()}, one per run of adjacent visible groups. */
	RenderPass.Draw<GpuBufferSlice[]>[] quillcore$faceDraws();

	int quillcore$faceMask();

	/** The whole-buffer draw the face draws were cut from; they are stale once it is replaced. */
	RenderPass.Draw<GpuBufferSlice[]> quillcore$faceSource();

	void quillcore$setFaceDraws(RenderPass.Draw<GpuBufferSlice[]>[] draws, int mask, RenderPass.Draw<GpuBufferSlice[]> source);

	int[] quillcore$faceGroups();

	void quillcore$setFaceGroups(int[] groups);
}
