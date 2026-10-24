package com.chopstickshq.quillcore.render.prep;

import com.chopstickshq.quillcore.render.compact.QuillBufferTag;
import com.chopstickshq.quillcore.render.mesh.FaceRuns;
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
	private static int lastDraws, lastBuilt, lastCulled;
	private static final int ALL_FACES = 0x3F;
	/** Slack for faces that sit exactly on the section boundary. */
	private static final double EDGE = 1.0 / 16.0;
	static final boolean FACE_CULL = !Boolean.getBoolean("quillcore.noFaceCull");

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
		int draws = 0, built = 0, culled = 0;
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
				int[] groups = FACE_CULL && range != null ? cache.quillcore$faceGroups() : null;
				int mask = groups == null ? ALL_FACES : faceMask(groups, o, cx, cy, cz);
				if (mask == ALL_FACES) {
					LISTS.get(layer).add(d);
					draws++;
					continue;
				}
				RenderPass.Draw<GpuBufferSlice[]>[] fd = cache.quillcore$faceDraws();
				if (fd == null || cache.quillcore$faceMask() != mask || cache.quillcore$faceSource() != d) {
					fd = faceDraws(d, groups, mask);
					cache.quillcore$setFaceDraws(fd, mask, d);
					built++;
				}
				List<RenderPass.Draw<GpuBufferSlice[]>> list = LISTS.get(layer);
				for (RenderPass.Draw<GpuBufferSlice[]> f : fd) list.add(f);
				draws += fd.length;
				culled += Integer.bitCount(~mask & ALL_FACES);
			}
		}
		GpuBufferSlice[] slices = RenderSystem.getDynamicUniforms().writeTransforms(Arrays.copyOf(transforms, used));
		lastDraws = draws;
		lastBuilt = built;
		lastCulled = culled;
		return new ChunkSectionsToRender(LISTS, maxIndices, slices);
	}

	/**
	 * Face groups that can face the camera: a +X face anywhere in the section is only front facing when the camera is
	 * past its plane, and those planes span [origin, origin + 16]. Empty groups count as visible so they never split a run.
	 */
	static int faceMask(int[] groups, BlockPos o, double cx, double cy, double cz) {
		double x = cx - o.getX(), y = cy - o.getY(), z = cz - o.getZ();
		int m = 0;
		if (groups[0] == 0 || x > -EDGE) m |= 1;
		if (groups[1] == 0 || x < 16 + EDGE) m |= 2;
		if (groups[2] == 0 || y > -EDGE) m |= 4;
		if (groups[3] == 0 || y < 16 + EDGE) m |= 8;
		if (groups[4] == 0 || z > -EDGE) m |= 16;
		if (groups[5] == 0 || z < 16 + EDGE) m |= 32;
		return m;
	}

	@SuppressWarnings("unchecked")
	private static RenderPass.Draw<GpuBufferSlice[]>[] faceDraws(RenderPass.Draw<GpuBufferSlice[]> d, int[] groups, int mask) {
		int[] runs = FaceRuns.runs(groups, mask);
		RenderPass.Draw<GpuBufferSlice[]>[] out = new RenderPass.Draw[runs.length / 2];
		for (int i = 0; i < out.length; i++) {
			out[i] = new RenderPass.Draw<>(0, d.vertexBuffer(), null, null, d.firstIndex() + runs[2 * i] * 6, runs[2 * i + 1] * 6,
					d.uniformUploaderConsumer());
		}
		return out;
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

	/** Face groups skipped last frame because they faced away from the camera. */
	public static int lastCulled() {
		return lastCulled;
	}

	/** Draws that had to be rebuilt last frame; the rest came from the per-buffer cache. */
	public static int lastBuilt() {
		return lastBuilt;
	}
}
