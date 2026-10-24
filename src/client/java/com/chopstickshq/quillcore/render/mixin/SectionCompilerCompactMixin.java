package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.render.compact.QuillMeshData;
import com.chopstickshq.quillcore.render.mesh.QuillMeshStats;
import com.chopstickshq.quillcore.render.mesh.QuillSectionTranscoder;
import com.chopstickshq.quillcore.render.region.RegionPos;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.SectionPos;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * After vanilla builds a section, re-encode its SOLID layer in place into the Quill compact format.
 * Any section that doesn't fully fit keeps its vanilla mesh untouched.
 */
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerCompactMixin {
	@Inject(method = "compile", at = @At("RETURN"))
	private void quillcore$compactSolid(SectionPos pos, RenderSectionRegion region, VertexSorting sorting, SectionBufferBuilderPack pack,
			CallbackInfoReturnable<SectionCompiler.Results> cir) {
		SectionCompiler.Results results = cir.getReturnValue();
		MeshData mesh = results.renderedLayers.get(ChunkSectionLayer.SOLID);
		if (mesh == null) return;
		MeshData.DrawState state = mesh.drawState();
		boolean quill = false;
		if (QuillConfig.get().compactMeshActive() && state.format() == DefaultVertexFormat.BLOCK && state.mode() == VertexFormat.Mode.QUADS && mesh.indexBuffer() == null) {
			ByteBuffer buf = mesh.vertexBuffer().order(ByteOrder.nativeOrder());
			quill = QuillSectionTranscoder.transcodeInPlace(buf, state.vertexCount(), RegionPos.slot(pos.x(), pos.y(), pos.z()));
		}
		if (quill) results.renderedLayers.put(ChunkSectionLayer.SOLID, new QuillMeshData(mesh));
		QuillMeshStats.recordSolid(state.vertexCount(), quill);
	}
}
