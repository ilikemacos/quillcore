package com.chopstickshq.quillcore.render.mesh;

import java.util.Arrays;

/** Turns per-face quad counts and a visible-face mask into contiguous (startQuad, quads) runs. */
public final class FaceRuns {
	private FaceRuns() {
	}

	/** @return pairs of start quad and quad count, one pair per run of adjacent visible, non-empty groups */
	public static int[] runs(int[] groups, int mask) {
		int[] tmp = new int[6];
		int n = 0, at = 0, start = -1, len = 0;
		for (int f = 0; f < 6; f++) {
			int q = groups[f];
			if ((mask >> f & 1) != 0) {
				if (q > 0) {
					if (start < 0) start = at;
					len += q;
				}
			} else if (q > 0 && start >= 0) {
				tmp[n++] = start;
				tmp[n++] = len;
				start = -1;
				len = 0;
			}
			at += q;
		}
		if (start >= 0) {
			tmp[n++] = start;
			tmp[n++] = len;
		}
		return Arrays.copyOf(tmp, n);
	}
}
