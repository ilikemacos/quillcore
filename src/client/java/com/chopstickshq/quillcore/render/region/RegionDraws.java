package com.chopstickshq.quillcore.render.region;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;
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
	private static final Reference2IntOpenHashMap<GpuBuffer> BUCKET = new Reference2IntOpenHashMap<>();
	private static Object[] heads = new Object[64];
	private static long[][] packed = new long[64][];
	private static int[] sizes = new int[64];
	private static int[] armed;
	private static int lastIn, lastOut, lastRanges;

	static {
		BUCKET.defaultReturnValue(-1);
	}

	private RegionDraws() {
	}

	public static <T> List<RenderPass.Draw<T>> merge(List<RenderPass.Draw<T>> draws) {
		// Bucket by slab buffer as the draws arrive: one packed (firstIndex, count) long per section, sorted as
		// primitives so neighbouring ranges still fold into one.
		BUCKET.clear();
		int buckets = 0;
		for (int i = 0, n = draws.size(); i < n; i++) {
			RenderPass.Draw<T> d = draws.get(i);
			int b = BUCKET.getInt(d.vertexBuffer());
			if (b < 0) {
				b = buckets++;
				BUCKET.put(d.vertexBuffer(), b);
				if (b == heads.length) {
					heads = Arrays.copyOf(heads, b * 2);
					packed = Arrays.copyOf(packed, b * 2);
					sizes = Arrays.copyOf(sizes, b * 2);
				}
				heads[b] = d;
				sizes[b] = 0;
				if (packed[b] == null) packed[b] = new long[8];
			}
			long[] p = packed[b];
			if (sizes[b] == p.length) packed[b] = p = Arrays.copyOf(p, p.length * 2);
			p[sizes[b]++] = (long) d.firstIndex() << 32 | (d.indexCount() & 0xFFFFFFFFL);
		}
		List<RenderPass.Draw<T>> out = new ArrayList<>(buckets);
		RANGES.clear();
		armed = null;
		int ranges = 0;
		for (int b = 0; b < buckets; b++) {
			@SuppressWarnings("unchecked")
			RenderPass.Draw<T> head = (RenderPass.Draw<T>) heads[b];
			heads[b] = null;
			int size = sizes[b];
			if (size == 1) {
				out.add(head);
				continue;
			}
			long[] p = packed[b];
			Arrays.sort(p, 0, size);
			BATCH.clear();
			for (int i = 0; i < size; i++) BATCH.add((int) p[i], p[i] >>> 32, 0, 1);
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
