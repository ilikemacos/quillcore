package com.chopstickshq.quillcore.logic;

import java.util.List;

/**
 * A stable sort of a list by a precomputed double key, giving exactly the order {@code List.sort} gives with a
 * {@code Double.compare} comparator on the same keys. Keys become longs that order the same way Double.compare
 * does (including -0.0 before 0.0 and NaN last), then a stable merge sort runs on primitive arrays, so no boxed
 * key objects are made.
 */
public final class DistanceSort {
	private DistanceSort() {}

	/** Maps a double to a long whose signed order is Double.compare's order. */
	public static long sortable(double d) {
		long b = Double.doubleToLongBits(d);
		return b ^ ((b >> 63) & Long.MAX_VALUE);
	}

	/** Sorts {@code list} in place so element i ends up ordered by {@code keys[i]}, stably. keys is consumed. */
	@SuppressWarnings("unchecked")
	public static <T> void sort(List<T> list, long[] keys) {
		int n = keys.length;
		int[] idx = new int[n];
		for (int i = 0; i < n; i++) idx[i] = i;
		long[] k2 = new long[n];
		int[] i2 = new int[n];
		final int run = 16;
		for (int lo = 0; lo < n; lo += run) {
			int hi = Math.min(lo + run, n);
			for (int i = lo + 1; i < hi; i++) {
				long k = keys[i];
				int v = idx[i];
				int j = i - 1;
				while (j >= lo && keys[j] > k) {
					keys[j + 1] = keys[j];
					idx[j + 1] = idx[j];
					j--;
				}
				keys[j + 1] = k;
				idx[j + 1] = v;
			}
		}
		long[] ka = keys, kb = k2;
		int[] ia = idx, ib = i2;
		for (int w = run; w < n; w <<= 1) {
			for (int lo = 0; lo < n; lo += w << 1) {
				int mid = Math.min(lo + w, n), hi = Math.min(lo + (w << 1), n);
				int a = lo, b = mid, o = lo;
				while (a < mid && b < hi) {
					if (ka[b] < ka[a]) { kb[o] = ka[b]; ib[o++] = ia[b++]; }
					else { kb[o] = ka[a]; ib[o++] = ia[a++]; }
				}
				while (a < mid) { kb[o] = ka[a]; ib[o++] = ia[a++]; }
				while (b < hi) { kb[o] = ka[b]; ib[o++] = ia[b++]; }
			}
			long[] tk = ka; ka = kb; kb = tk;
			int[] ti = ia; ia = ib; ib = ti;
		}
		Object[] src = list.toArray();
		for (int i = 0; i < n; i++) list.set(i, (T) src[ia[i]]);
	}
}
