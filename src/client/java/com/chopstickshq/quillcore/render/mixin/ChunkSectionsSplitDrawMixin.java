package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.compact.QuillBufferTag;
import com.chopstickshq.quillcore.render.compact.QuillTerrainPipeline;
import com.chopstickshq.quillcore.render.region.RegionDraws;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Splits each layer's batched section draw into a vanilla sub-batch (pipeline already set by vanilla) and a Quill
 * sub-batch drawn with {@link QuillTerrainPipeline#SOLID}. Only SOLID buffers are ever tagged compact, so other
 * layers and an all-vanilla SOLID list take the unchanged single call. Compact draws are folded per region slab.
 */
@Mixin(ChunkSectionsToRender.class)
public abstract class ChunkSectionsSplitDrawMixin {
	@WrapOperation(method = "renderGroup", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderPass;drawMultipleIndexed(Ljava/util/Collection;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;Ljava/util/Collection;Ljava/lang/Object;)V"))
	private void quillcore$splitDraw(RenderPass pass, Collection<RenderPass.Draw<GpuBufferSlice[]>> draws, GpuBuffer indexBuffer,
			VertexFormat.IndexType indexType, Collection<String> dynamicUniforms, Object uniformArgs, Operation<Void> original) {
		List<RenderPass.Draw<GpuBufferSlice[]>> compact = null;
		List<RenderPass.Draw<GpuBufferSlice[]>> vanilla = null;
		for (RenderPass.Draw<GpuBufferSlice[]> d : draws) {
			if (QuillBufferTag.isCompact(d.vertexBuffer())) {
				if (compact == null) {
					compact = new ArrayList<>();
					vanilla = new ArrayList<>(draws.size());
					for (RenderPass.Draw<GpuBufferSlice[]> prev : draws) {
						if (prev == d) break;
						vanilla.add(prev);
					}
				}
				compact.add(d);
			} else if (vanilla != null) {
				vanilla.add(d);
			}
		}
		if (compact == null) {
			quillcore$draw(original, pass, draws, indexBuffer, indexType, dynamicUniforms, uniformArgs);
			return;
		}
		if (!vanilla.isEmpty()) quillcore$draw(original, pass, vanilla, indexBuffer, indexType, dynamicUniforms, uniformArgs);
		pass.setPipeline(QuillTerrainPipeline.SOLID);
		quillcore$draw(original, pass, RegionDraws.merge(compact), indexBuffer, indexType, dynamicUniforms, uniformArgs);
	}

	private static void quillcore$draw(Operation<Void> original, RenderPass pass, Collection<RenderPass.Draw<GpuBufferSlice[]>> draws, GpuBuffer indexBuffer,
			VertexFormat.IndexType indexType, Collection<String> dynamicUniforms, Object uniformArgs) {
		original.call(pass, draws, indexBuffer, indexType, dynamicUniforms, uniformArgs);
	}
}
