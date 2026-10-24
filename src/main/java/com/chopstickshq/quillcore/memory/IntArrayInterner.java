package com.chopstickshq.quillcore.memory;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Hands back one shared array for equal contents. Used for baked quad vertex data, where many
 * models bake identical faces. Callers must treat the returned arrays as read-only.
 */
public final class IntArrayInterner {
	private final ConcurrentHashMap<Key, int[]> pool = new ConcurrentHashMap<>();
	private final LongAdder seen = new LongAdder();

	public int[] intern(int[] data) {
		seen.increment();
		int[] prev = pool.putIfAbsent(new Key(data), data);
		return prev != null ? prev : data;
	}

	/** Arrays passed in since the last clear. */
	public long seen() {
		return seen.sum();
	}

	public int size() {
		return pool.size();
	}

	public void clear() {
		pool.clear();
		seen.reset();
	}

	private record Key(int[] data, int hash) {
		Key(int[] data) {
			this(data, Arrays.hashCode(data));
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && k.hash == hash && Arrays.equals(k.data, data);
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}
}
