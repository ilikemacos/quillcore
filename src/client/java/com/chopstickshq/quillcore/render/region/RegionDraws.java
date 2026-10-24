package com.chopstickshq.quillcore.render.region;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

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
	/** Ranges behind each multi-range draw handed out by the last merge: {n, first0, count0, first1, count1, ...}. */
	private static final Reference2ObjectOpenHashMap<RenderPass.Draw<?>, int[]> RANGES = new Reference2ObjectOpenHashMap<>();
	private static int[] armed;
	private static int lastIn, lastOut, lastRanges;

	private RegionDraws() {
	}

	public static <T> List<RenderPass.Draw<T>> merge(List<RenderPass.Draw<T>> draws) {
		Reference2ObjectLinkedOpenHashMap<GpuBuffer, List<RenderPass.Draw<T>>> byBuffer = new Reference2ObjectLinkedOpenHashMap<>();
		for (RenderPass.Draw<T> d : draws) byBuffer.computeIfAbsent(d.vertexBuffer(), k -> new ArrayList<>()).add(d);
		List<RenderPass.Draw<T>> out = new ArrayList<>(byBuffer.size());
		RANGES.clear();
		armed = null;
		int ranges = 0;
		for (List<RenderPass.Draw<T>> group : byBuffer.values()) {
			if (group.size() == 1) {
				out.add(group.get(0));
				continue;
			}
			group.sort(Comparator.comparingInt(RenderPass.Draw::firstIndex));
			RenderPass.Draw<T> head = group.get(0);
			BATCH.clear();
			for (RenderPass.Draw<T> d : group) BATCH.add(d.indexCount(), d.firstIndex(), 0, 1);
			// One draw per region. It names the first range; the encoder hook swaps its single call for a multidraw.
			RenderPass.Draw<T> d = new RenderPass.Draw<>(head.slot(), head.vertexBuffer(), head.indexBuffer(), head.indexType(),
					(int) BATCH.offset(0), BATCH.count(0), head.uniformUploaderConsumer());
			out.add(d);
			ranges += BATCH.size();
			if (BATCH.size() > 1) {
				int[] r = new int[1 + 2 * BATCH.size()];
				r[0] = BATCH.size();
				for (int i = 0; i < BATCH.size(); i++) {
					r[1 + 2 * i] = (int) BATCH.offset(i);
					r[2 + 2 * i] = BATCH.count(i);
				}
				RANGES.put(d, r);
			}
		}
		lastIn = draws.size();
		lastOut = out.size();
		lastRanges = ranges;
		return out;
	}

	/** Called as the encoder reaches a draw: remembers its ranges, if it has several, for the next element call. */
	public static void arm(RenderPass.Draw<?> d) {
		armed = RANGES.isEmpty() ? null : RANGES.get(d);
	}

	/** Ranges for the element call about to run, or null for a plain call. Clears the armed state. */
	public static int[] takeArmed() {
		int[] r = armed;
		armed = null;
		return r;
	}

	/** Index ranges behind the merged draws of the last merge (single-section regions not counted). */
	public static int lastRanges() {
		return lastRanges;
	}

	public static int lastIn() {
		return lastIn;
	}

	public static int lastOut() {
		return lastOut;
	}
}
