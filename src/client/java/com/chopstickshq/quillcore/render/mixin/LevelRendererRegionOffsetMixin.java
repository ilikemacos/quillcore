package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.compact.QuillBufferTag;
import com.chopstickshq.quillcore.render.region.RegionPos;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Compact SOLID vertices carry their section's slot and the shader adds the slot offset, so their ModelOffset has to
 * be the region origin instead of the section origin. Vanilla buffers keep the section origin.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererRegionOffsetMixin {
	@ModifyArg(method = "prepareChunkRenders", index = 2, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/DynamicUniforms$Transform;<init>(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;F)V"))
	private Vector3fc quillcore$regionOffset(Vector3fc offset, @Local SectionBuffers buffers, @Local SectionRenderDispatcher.RenderSection section) {
		if (!QuillBufferTag.isCompact(buffers.getVertexBuffer())) return offset;
		BlockPos o = section.getRenderOrigin();
		int slot = RegionPos.slot(o.getX() >> 4, o.getY() >> 4, o.getZ() >> 4);
		return new Vector3f(offset.x() - (slot & 7) * 16, offset.y() - (slot >> 6) * 16, offset.z() - (slot >> 3 & 7) * 16);
	}
}
