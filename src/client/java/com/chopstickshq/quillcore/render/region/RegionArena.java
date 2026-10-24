package com.chopstickshq.quillcore.render.region;

import java.util.Map;
import java.util.TreeMap;

/**
 * Hands out byte ranges of one region buffer. First fit over a free list keyed by offset;
 * freed ranges merge with their neighbours. Grows by doubling when nothing fits.
 * Not thread safe, owned by the render thread.
 */
public final class RegionArena {
	private final int align;
	private final TreeMap<Integer, Integer> free = new TreeMap<>();
	private int capacity;
	private int used;

	public RegionArena(int capacity, int align) {
		if (capacity <= 0 || align <= 0) throw new IllegalArgumentException();
		this.align = align;
		this.capacity = capacity;
		free.put(0, capacity);
	}

	/** Offset of a fresh range of at least {@code size} bytes, growing the arena if needed. */
	public int alloc(int size) {
		int want = round(size);
		for (Map.Entry<Integer, Integer> e : free.entrySet()) {
			if (e.getValue() >= want) return take(e.getKey(), e.getValue(), want);
		}
		grow(want);
		return alloc(size);
	}

	public void free(int offset, int size) {
		int len = round(size);
		Map.Entry<Integer, Integer> lo = free.floorEntry(offset);
		if (lo != null && lo.getKey() + lo.getValue() > offset) throw new IllegalStateException("double free at " + offset);
		used -= len;
		Integer next = free.get(offset + len);
		if (next != null) {
			free.remove(offset + len);
			len += next;
		}
		if (lo != null && lo.getKey() + lo.getValue() == offset) {
			free.put(lo.getKey(), lo.getValue() + len);
		} else {
			free.put(offset, len);
		}
	}

	public int capacity() {
		return capacity;
	}

	public int used() {
		return used;
	}

	/** Number of free holes; 1 means no fragmentation. */
	public int holes() {
		return free.size();
	}

	private int take(int off, int len, int want) {
		free.remove(off);
		if (len > want) free.put(off + want, len - want);
		used += want;
		return off;
	}

	private void grow(int want) {
		int old = capacity;
		int cap = old;
		while (cap - old < want) cap *= 2;
		capacity = cap;
		// the copy into the bigger buffer keeps offsets, so the new space is just a hole at the end
		Map.Entry<Integer, Integer> last = free.lastEntry();
		if (last != null && last.getKey() + last.getValue() == old) {
			free.put(last.getKey(), last.getValue() + cap - old);
		} else {
			free.put(old, cap - old);
		}
	}

	private int round(int size) {
		if (size <= 0) throw new IllegalArgumentException("size " + size);
		return (size + align - 1) / align * align;
	}
}
