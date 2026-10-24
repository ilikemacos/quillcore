package com.chopstickshq.quillcore.logic;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

/**
 * Which sections of each chunk column hold POI storage, one bit per section (bit 0 = lowest section).
 * Lets a POI search skip the per-section hash lookups for a whole column at once.
 * Columns not seen yet read as {@link #UNKNOWN}, and callers fall back to the full walk.
 * Worlds taller than 63 sections are not tracked at all.
 */
public final class PoiColumns {
	public static final long UNKNOWN = Long.MIN_VALUE;
	public static final int MAX_SECTIONS = 63;

	private final Long2LongOpenHashMap masks = new Long2LongOpenHashMap();

	public PoiColumns() {
		masks.defaultReturnValue(UNKNOWN);
	}

	/** Replaces the column's mask after it was read from disk or generated empty. */
	public void column(long chunk, long mask) {
		masks.put(chunk, mask);
	}

	/** One section of a known column now has storage. Unknown columns stay unknown. */
	public void section(long chunk, int index) {
		long m = masks.get(chunk);
		if (m != UNKNOWN) masks.put(chunk, m | 1L << index);
	}

	public long mask(long chunk) {
		return masks.get(chunk);
	}

	public int size() {
		return masks.size();
	}
}
