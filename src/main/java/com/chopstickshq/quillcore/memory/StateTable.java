package com.chopstickshq.quillcore.memory;

/**
 * Shared neighbour lookup for every state of one block.
 *
 * Vanilla gives each state its own map of property -> neighbour array. All the states of a
 * block form a grid though, so one flat array per block is enough: a state is a cell index,
 * and changing one property moves the index by that property's stride.
 */
public final class StateTable<K, S> {
	private final K[] keys;
	private final int[] strides;
	private final Object[] states;

	public StateTable(K[] keys, int[] sizes) {
		this.keys = keys;
		this.strides = new int[sizes.length];
		int n = 1;
		for (int i = sizes.length - 1; i >= 0; i--) {
			strides[i] = n;
			n = Math.multiplyExact(n, sizes[i]);
		}
		this.states = new Object[n];
	}

	public int index(int[] ordinals) {
		int idx = 0;
		for (int i = 0; i < ordinals.length; i++) {
			idx += ordinals[i] * strides[i];
		}
		return idx;
	}

	public void put(int index, S state) {
		states[index] = state;
	}

	public int size() {
		return states.length;
	}

	@SuppressWarnings("unchecked")
	public S neighbour(int index, K key, int from, int to) {
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] == key) {
				return (S) states[index + (to - from) * strides[i]];
			}
		}
		throw new IllegalArgumentException("unknown key " + key);
	}
}
