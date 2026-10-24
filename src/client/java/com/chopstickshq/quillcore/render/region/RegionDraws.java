package com.chopstickshq.quillcore.render.region;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Folds the per-section draws of one region slab into as few draws as the slab layout allows. Every section of a
 * region carries the same ModelOffset (region origin minus camera, the slot is added in the shader), so a merged draw
 * can reuse the uniforms of its first section. Render thread only.
 */
public final class RegionDraws {
	private static final MultiDrawBatch BATCH = new MultiDrawBatch();
	private static int lastIn, lastOut;

	private RegionDraws() {
	}

	public static <T> List<RenderPass.Draw<T>> merge(List<RenderPass.Draw<T>> draws) {
		Reference2ObjectLinkedOpenHashMap<GpuBuffer, List<RenderPass.Draw<T>>> byBuffer = new Reference2ObjectLinkedOpenHashMap<>();
		for (RenderPass.Draw<T> d : draws) byBuffer.computeIfAbsent(d.vertexBuffer(), k -> new ArrayList<>()).add(d);
		List<RenderPass.Draw<T>> out = new ArrayList<>(byBuffer.size());
		for (List<RenderPass.Draw<T>> group : byBuffer.values()) {
			if (group.size() == 1) {
				out.add(group.get(0));
				continue;
			}
			group.sort(Comparator.comparingInt(RenderPass.Draw::firstIndex));
			RenderPass.Draw<T> head = group.get(0);
			BATCH.clear();
			for (RenderPass.Draw<T> d : group) BATCH.add(d.indexCount(), d.firstIndex(), 0, 1);
			for (int i = 0; i < BATCH.size(); i++) {
				out.add(new RenderPass.Draw<>(head.slot(), head.vertexBuffer(), head.indexBuffer(), head.indexType(),
						(int) BATCH.offset(i), BATCH.count(i), head.uniformUploaderConsumer()));
			}
		}
		lastIn = draws.size();
		lastOut = out.size();
		return out;
	}

	public static int lastIn() {
		return lastIn;
	}

	public static int lastOut() {
		return lastOut;
	}
}
