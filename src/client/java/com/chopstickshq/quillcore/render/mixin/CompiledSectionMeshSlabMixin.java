package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.compact.QuillMeshData;
import com.chopstickshq.quillcore.render.prep.DrawCached;

import com.chopstickshq.quillcore.render.compact.QuillTerrainPipeline;
import com.chopstickshq.quillcore.render.region.RegionSlabs;
import com.chopstickshq.quillcore.render.region.SlabBacked;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Compact SOLID meshes go into their region's slab instead of a buffer of their own. Any upload over a slab backed
 * layer drops the old range first, so vanilla never writes through the shared buffer.
 */
@Mixin(CompiledSectionMesh.class)
public abstract class CompiledSectionMeshSlabMixin {
	@Shadow
	@Final
	private Map<ChunkSectionLayer, SectionBuffers> buffers;

	@Inject(method = "uploadMeshLayer", at = @At("HEAD"), cancellable = true)
	private void quillcore$uploadToSlab(ChunkSectionLayer layer, MeshData mesh, long node, CallbackInfo ci) {
		SectionBuffers old = buffers.get(layer);
		boolean slab = layer == ChunkSectionLayer.SOLID && mesh.drawState().format() == QuillTerrainPipeline.FORMAT && mesh.indexBuffer() == null;
		if (old != null && (slab || SlabBacked.rangeOf(old) != null)) {
			buffers.remove(layer);
			old.close();
		}
		if (!slab) return;
		RegionSlabs.Range r = RegionSlabs.upload(SectionPos.x(node), SectionPos.y(node), SectionPos.z(node), mesh.vertexBuffer());
		SectionBuffers b = new SectionBuffers(null, null, mesh.drawState().indexCount(), mesh.drawState().indexType());
		((SlabBacked) (Object) b).quillcore$setRange(r);
		if (mesh instanceof QuillMeshData q) ((DrawCached) (Object) b).quillcore$setFaceGroups(q.faceGroups());
		buffers.put(layer, b);
		ci.cancel();
	}
}
