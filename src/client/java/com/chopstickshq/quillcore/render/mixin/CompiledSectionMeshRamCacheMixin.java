package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.ramcache.MeshCacheHook;
import com.chopstickshq.quillcore.render.ramcache.RamCached;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * feeds non-indexed, non-slab section uploads to the ram mesh cache, and brings
 * evicted ones back when they are drawn again. translucent (indexed) layers
 * are left alone.
 */
@Mixin(CompiledSectionMesh.class)
public abstract class CompiledSectionMeshRamCacheMixin {
	@Shadow
	@Final
	private Map<ChunkSectionLayer, SectionBuffers> buffers;

	@Inject(method = "uploadMeshLayer", at = @At("HEAD"))
	private void quillcore$dropEvicted(ChunkSectionLayer layer, MeshData mesh, long node, CallbackInfo ci) {
		SectionBuffers old = buffers.get(layer);
		if (old != null && MeshCacheHook.evicted(id(old))) buffers.remove(layer).close();
	}

	@Inject(method = "uploadMeshLayer", at = @At("TAIL"))
	private void quillcore$track(ChunkSectionLayer layer, MeshData mesh, long node, CallbackInfo ci) {
		SectionBuffers b = buffers.get(layer);
		if (b == null) return;
		if (mesh.indexBuffer() != null) MeshCacheHook.untrack(id(b));
		else MeshCacheHook.uploaded(b, id(b), mesh.vertexBuffer());
	}

	@Inject(method = "getBuffers", at = @At("HEAD"), cancellable = true)
	private void quillcore$restore(ChunkSectionLayer layer, CallbackInfoReturnable<SectionBuffers> cir) {
		SectionBuffers b = buffers.get(layer);
		if (b != null && !MeshCacheHook.ready(b, id(b))) cir.setReturnValue(null);
	}

	private static long id(SectionBuffers b) {
		return ((RamCached) (Object) b).quillcore$cacheId();
	}
}
