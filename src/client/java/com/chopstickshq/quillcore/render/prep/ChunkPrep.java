package com.chopstickshq.quillcore.render.prep;

import com.chopstickshq.quillcore.render.compact.QuillBufferTag;
import com.chopstickshq.quillcore.render.region.RegionPos;
import com.chopstickshq.quillcore.render.region.RegionSlabs;
import com.chopstickshq.quillcore.render.region.SlabBacked;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Builds the frame's section draws the same way vanilla does, minus the garbage: one transform per section (layers
 * share it) or per region for compact buffers, transforms and their offset vectors pooled by slot, the per-slot
 * uniform lambdas built once, and each section's Draw kept until its buffer, range or slot changes. The per-layer
 * lists are reused every frame. Render thread only.
 */
public final class ChunkPrep {
	private static final ChunkSectionLayer[] LAYERS = ChunkSectionLayer.values();
	private static final EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> LISTS = new EnumMap<>(ChunkSectionLayer.class);
	private static final Vector4f COLOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
	private static final Matrix4f TEXTURE = new Matrix4f();
	private static final Matrix4f MODEL_VIEW = new Matrix4f();
	private static final Long2IntOpenHashMap REGION_SLOT = new Long2IntOpenHashMap();

	private static Vector3f[] offsets = new Vector3f[0];
	private static DynamicUniforms.Transform[] transforms = new DynamicUniforms.Transform[0];
	@SuppressWarnings("unchecked")
	private static BiConsumer<GpuBufferSlice[], RenderPass.UniformUploader>[] uploaders = new BiConsumer[0];
	private static int used;
	private static int lastDraws, lastBuilt;

	static {
		for (ChunkSectionLayer l : LAYERS) LISTS.put(l, new ArrayList<>());
		REGION_SLOT.defaultReturnValue(-1);
	}

	private ChunkPrep() {
	}

	public static ChunkSectionsToRender prepare(ObjectArrayList<SectionRenderDispatcher.RenderSection> visible, Matrix4fc modelView,
			double cx, double cy, double cz) {
		MODEL_VIEW.set(modelView);
		for (List<RenderPass.Draw<GpuBufferSlice[]>> l : LISTS.values()) l.clear();
		REGION_SLOT.clear();
		used = 0;
		int draws = 0, built = 0;
		int maxIndices = 0;
		Object[] sections = visible.elements();
		for (int s = 0, n = visible.size(); s < n; s++) {
			SectionRenderDispatcher.RenderSection section = (SectionRenderDispatcher.RenderSection) sections[s];
			SectionMesh mesh = section.getSectionMesh();
			BlockPos o = section.getRenderOrigin();
			int sectionSlot = -1;
			for (ChunkSectionLayer layer : LAYERS) {
				SectionBuffers buffers = mesh.getBuffers(layer);
				if (buffers == null) continue;
				GpuBuffer vertex = buffers.getVertexBuffer();
				GpuBuffer index = buffers.getIndexBuffer();
				int count = buffers.getIndexCount();
				int first = 0;
				RegionSlabs.Range range = SlabBacked.rangeOf(buffers);
				if (range != null) first = range.firstIndex();
				if (index == null) maxIndices = Math.max(maxIndices, range != null ? first + count : count);

				int slot;
				if (QuillBufferTag.isCompact(vertex)) {
					slot = regionSlot(o, cx, cy, cz);
				} else {
					if (sectionSlot < 0) sectionSlot = take(o.getX() - cx, o.getY() - cy, o.getZ() - cz);
					slot = sectionSlot;
				}

				DrawCached cache = (DrawCached) (Object) buffers;
				RenderPass.Draw<GpuBufferSlice[]> d = cache.quillcore$draw();
				BiConsumer<GpuBufferSlice[], RenderPass.UniformUploader> up = uploaders[slot];
				if (d == null || d.vertexBuffer() != vertex || d.indexBuffer() != index || d.indexCount() != count
						|| d.firstIndex() != first || d.uniformUploaderConsumer() != up) {
					VertexFormat.IndexType type = index == null ? null : buffers.getIndexType();
					d = new RenderPass.Draw<>(0, vertex, index, type, first, count, up);
					cache.quillcore$setDraw(d);
					built++;
				}
				LISTS.get(layer).add(d);
				draws++;
			}
		}
		GpuBufferSlice[] slices = RenderSystem.getDynamicUniforms().writeTransforms(Arrays.copyOf(transforms, used));
		lastDraws = draws;
		lastBuilt = built;
		return new ChunkSectionsToRender(LISTS, maxIndices, slices);
	}

	/** Compact vertices add their slot offset in the shader, so the whole region shares the region origin's transform. */
	private static int regionSlot(BlockPos o, double cx, double cy, double cz) {
		int sx = o.getX() >> 4, sy = o.getY() >> 4, sz = o.getZ() >> 4;
		long region = RegionPos.of(sx, sy, sz);
		int slot = REGION_SLOT.get(region);
		if (slot >= 0) return slot;
		int r = RegionPos.slot(sx, sy, sz);
		slot = take(o.getX() - (r & 7) * 16 - cx, o.getY() - (r >> 6) * 16 - cy, o.getZ() - (r >> 3 & 7) * 16 - cz);
		REGION_SLOT.put(region, slot);
		return slot;
	}

	private static int take(double x, double y, double z) {
		int j = used++;
		if (j == transforms.length) grow();
		offsets[j].set((float) x, (float) y, (float) z);
		return j;
	}

	@SuppressWarnings("unchecked")
	private static void grow() {
		int old = transforms.length;
		int n = Math.max(256, old * 2);
		offsets = Arrays.copyOf(offsets, n);
		transforms = Arrays.copyOf(transforms, n);
		uploaders = Arrays.copyOf(uploaders, n);
		for (int j = old; j < n; j++) {
			Vector3f v = new Vector3f();
			offsets[j] = v;
			transforms[j] = new DynamicUniforms.Transform(MODEL_VIEW, COLOR, v, TEXTURE, 1.0F);
			int slot = j;
			uploaders[j] = (slices, uploader) -> uploader.upload("DynamicTransforms", slices[slot]);
		}
	}

	public static int lastDraws() {
		return lastDraws;
	}

	/** Draws that had to be rebuilt last frame; the rest came from the per-buffer cache. */
	public static int lastBuilt() {
		return lastBuilt;
	}
}
