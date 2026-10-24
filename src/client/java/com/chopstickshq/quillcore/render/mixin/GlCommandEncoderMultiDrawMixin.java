package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.region.RegionDraws;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderPass;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL14;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

/**
 * Draws a merged region draw that still covers several index ranges with one glMultiDrawElements instead of one
 * glDrawElements per range. Draws RegionDraws did not split are left alone, so vanilla draws never take this path.
 */
@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMultiDrawMixin {
	@Redirect(method = "executeDrawMultiple", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderPass$Draw;indexCount()I"))
	private int quillcore$armRanges(RenderPass.Draw<?> draw) {
		RegionDraws.arm(draw);
		return draw.indexCount();
	}

	@Redirect(method = "drawFromBuffers", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_drawElements(IIIJ)V"))
	private void quillcore$multiDraw(int mode, int count, int type, long offset) {
		int[] r = RegionDraws.takeArmed();
		if (r == null) {
			GlStateManager._drawElements(mode, count, type, offset);
			return;
		}
		long bytes = indexBytes(type);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer counts = stack.mallocInt(r[0]);
			PointerBuffer offsets = stack.mallocPointer(r[0]);
			for (int i = 0; i < r[0]; i++) {
				counts.put(i, r[2 + 2 * i]);
				offsets.put(i, r[1 + 2 * i] * bytes);
			}
			GL14.glMultiDrawElements(mode, counts, type, offsets);
		}
	}

	private static long indexBytes(int glType) {
		return switch (glType) {
			case 0x1401 -> 1; // GL_UNSIGNED_BYTE
			case 0x1403 -> 2; // GL_UNSIGNED_SHORT
			default -> 4;
		};
	}
}
