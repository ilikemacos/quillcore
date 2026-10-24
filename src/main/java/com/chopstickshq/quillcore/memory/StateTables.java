package com.chopstickshq.quillcore.memory;

import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

/** Builds one {@link StateTable} per state definition and hands it to each of its states. */
public final class StateTables {
	private static Map<?, ?> lastSource;
	private static StateTable<Property<?>, ?> lastTable;

	private StateTables() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static synchronized <S> StateTable<Property<?>, S> forDefinition(
			Map<Map<Property<?>, Comparable<?>>, S> all, Map<Property<?>, Comparable<?>> sample) {
		// populateNeighbours gets the same map instance for every state of a definition
		if (all == lastSource) {
			return (StateTable<Property<?>, S>) lastTable;
		}
		Property<?>[] keys = sample.keySet().toArray(new Property<?>[0]);
		int[] sizes = new int[keys.length];
		for (int i = 0; i < keys.length; i++) {
			sizes[i] = keys[i].getPossibleValues().size();
		}
		StateTable<Property<?>, S> table = new StateTable<>(keys, sizes);
		for (Map.Entry<Map<Property<?>, Comparable<?>>, S> e : all.entrySet()) {
			table.put(indexOf(table, keys, e.getKey()), e.getValue());
		}
		lastSource = all;
		lastTable = table;
		return table;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static int indexOf(StateTable<Property<?>, ?> table, Property<?>[] keys, Map<Property<?>, Comparable<?>> values) {
		int[] ords = new int[keys.length];
		for (int i = 0; i < keys.length; i++) {
			Property p = keys[i];
			ords[i] = p.getInternalIndex((Comparable) values.get(p));
		}
		return table.index(ords);
	}

	public static <S> int indexOf(StateTable<Property<?>, S> table, Map<Property<?>, Comparable<?>> values) {
		return indexOf(table, values.keySet().toArray(new Property<?>[0]), values);
	}
}
