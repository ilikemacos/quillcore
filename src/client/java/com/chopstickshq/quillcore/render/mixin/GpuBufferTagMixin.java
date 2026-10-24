package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.compact.QuillBufferTag;
import com.mojang.blaze3d.buffers.GpuBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GpuBuffer.class)
public abstract class GpuBufferTagMixin implements QuillBufferTag {
	@Unique
	private volatile boolean quillcore$compact;

	@Override
	public boolean quillcore$isCompact() {
		return quillcore$compact;
	}

	@Override
	public void quillcore$setCompact(boolean compact) {
		quillcore$compact = compact;
	}
}
