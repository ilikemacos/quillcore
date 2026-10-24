package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.compact.QuillBufferTag;
import com.chopstickshq.quillcore.render.compact.QuillTerrainPipeline;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Upload is format-agnostic; after every layer upload, record on the (possibly reused) vertex buffer whether it now
 * holds Quill-format data, so the draw side picks the matching pipeline per buffer.
 */
@Mixin(CompiledSectionMesh.class)
public abstract class CompiledSectionMeshTagMixin {
	@Inject(method = "uploadMeshLayer", at = @At("TAIL"))
	private void quillcore$tagVertexBuffer(ChunkSectionLayer layer, MeshData meshData, long sectionNode, CallbackInfo ci) {
		SectionBuffers buffers = ((CompiledSectionMesh) (Object) this).getBuffers(layer);
		if (buffers != null && buffers.getVertexBuffer() instanceof QuillBufferTag tag) {
			tag.quillcore$setCompact(meshData.drawState().format() == QuillTerrainPipeline.FORMAT);
		}
	}
}
