package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.compact.QuillBufferTag;
import com.chopstickshq.quillcore.render.region.RegionPos;
import com.chopstickshq.quillcore.render.region.RegionSlabs;
import com.chopstickshq.quillcore.render.region.SlabBacked;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

	@Unique
	private int quillcore$slabIndices;

	@Inject(method = "prepareChunkRenders", at = @At("HEAD"))
	private void quillcore$resetSlabIndices(CallbackInfoReturnable<?> cir) {
		quillcore$slabIndices = 0;
	}

	/** Slab backed sections start at their range inside the region buffer, read through the sequential quad indices. */
	@ModifyArg(method = "prepareChunkRenders", index = 4, at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderPass$Draw;<init>(ILcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;IILjava/util/function/BiConsumer;)V"))
	private int quillcore$slabFirstIndex(int firstIndex, @Local SectionBuffers buffers) {
		RegionSlabs.Range r = SlabBacked.rangeOf(buffers);
		if (r == null) return firstIndex;
		int first = r.firstIndex();
		quillcore$slabIndices = Math.max(quillcore$slabIndices, first + buffers.getIndexCount());
		return first;
	}

	@ModifyArg(method = "prepareChunkRenders", index = 1, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;<init>(Ljava/util/EnumMap;I[Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
	private int quillcore$slabMaxIndices(int max) {
		return Math.max(max, quillcore$slabIndices);
	}
}
