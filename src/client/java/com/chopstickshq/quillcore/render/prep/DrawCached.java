package com.chopstickshq.quillcore.render.prep;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;

/** Last draw built for a section buffer, reused while every field it was built from still matches. */
public interface DrawCached {
	RenderPass.Draw<GpuBufferSlice[]> quillcore$draw();

	void quillcore$setDraw(RenderPass.Draw<GpuBufferSlice[]> draw);
}
