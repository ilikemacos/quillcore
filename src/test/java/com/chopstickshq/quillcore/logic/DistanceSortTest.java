package com.chopstickshq.quillcore.logic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistanceSortTest {
	private static final double[] ODD = {0.0, -0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE, -1.5, 1.5};

	@Test
	void sameOrderAsStableListSort() {
		Random r = new Random(7);
		for (int trial = 0; trial < 3000; trial++) {
			int n = r.nextInt(trial < 2000 ? 40 : 300);
			double[] d = new double[n];
			int distinct = 1 + r.nextInt(Math.max(1, n));
			for (int i = 0; i < n; i++) d[i] = r.nextInt(10) == 0 ? ODD[r.nextInt(ODD.length)] : r.nextInt(distinct) * 0.25;
			List<Integer> want = new ArrayList<>();
			for (int i = 0; i < n; i++) want.add(i);
			want.sort(Comparator.comparingDouble(i -> d[i]));
			List<Integer> got = new ArrayList<>();
			long[] k = new long[n];
			for (int i = 0; i < n; i++) { got.add(i); k[i] = DistanceSort.sortable(d[i]); }
			DistanceSort.sort(got, k);
			assertEquals(want, got, "n=" + n + " trial=" + trial);
		}
	}

	@Test
	void sortableMatchesDoubleCompare() {
		for (double a : ODD) for (double b : ODD)
			assertEquals(Integer.signum(Double.compare(a, b)), Integer.signum(Long.compare(DistanceSort.sortable(a), DistanceSort.sortable(b))), a + " " + b);
	}
}
