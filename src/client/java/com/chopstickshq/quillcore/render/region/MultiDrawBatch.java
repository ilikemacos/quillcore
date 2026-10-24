package com.chopstickshq.quillcore.render.region;

import java.util.Arrays;

/**
 * Per region draw lists in the shape glMultiDrawElementsBaseVertex takes: index counts,
 * index byte offsets and base vertices. Reused every frame.
 */
public final class MultiDrawBatch {
	private int[] counts = new int[16];
	private long[] offsets = new long[16];
	private int[] bases = new int[16];
	private int size;

	public void clear() {
		size = 0;
	}

	/** Adds a draw; a draw that starts right where the previous one ended with the same base is merged. */
	public void add(int indexCount, long indexOffset, int baseVertex, int indexBytes) {
		if (indexCount <= 0) return;
		if (size > 0 && bases[size - 1] == baseVertex && offsets[size - 1] + (long) counts[size - 1] * indexBytes == indexOffset) {
			counts[size - 1] += indexCount;
			return;
		}
		if (size == counts.length) {
			int n = size * 2;
			counts = Arrays.copyOf(counts, n);
			offsets = Arrays.copyOf(offsets, n);
			bases = Arrays.copyOf(bases, n);
		}
		counts[size] = indexCount;
		offsets[size] = indexOffset;
		bases[size] = baseVertex;
		size++;
	}

	public int size() {
		return size;
	}

	public int count(int i) {
		return counts[i];
	}

	public long offset(int i) {
		return offsets[i];
	}

	public int base(int i) {
		return bases[i];
	}
}
