package com.chopstickshq.quillcore.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Pending-change buffer for the settings screen. Controls read/write the staged value; nothing touches
 * the live option until {@link #apply()}. {@link #undo()} drops everything pending.
 * Pure — the live side is just a getter/setter pair per option.
 */
public final class StagedSettings {
	private record Binding<T>(Supplier<T> read, Consumer<T> write) {}

	private final Map<String, Binding<?>> bindings = new LinkedHashMap<>();
	private final Map<String, Object> pending = new LinkedHashMap<>();

	public <T> void bind(String id, Supplier<T> read, Consumer<T> write) {
		if (bindings.putIfAbsent(id, new Binding<>(read, write)) != null) {
			throw new IllegalArgumentException("Duplicate setting id " + id);
		}
	}

	/** Staged value if any, else the live value. */
	@SuppressWarnings("unchecked")
	public <T> T get(String id) {
		if (pending.containsKey(id)) {
			return (T) pending.get(id);
		}
		return (T) binding(id).read().get();
	}

	/** Stage a value; staging the live value again clears the pending entry. */
	public void stage(String id, Object value) {
		Object live = binding(id).read().get();
		if (Objects.equals(live, value)) {
			pending.remove(id);
		} else {
			pending.put(id, value);
		}
	}

	public boolean isPending(String id) {
		return pending.containsKey(id);
	}

	public boolean hasPending() {
		return !pending.isEmpty();
	}

	public int pendingCount() {
		return pending.size();
	}

	public void undo() {
		pending.clear();
	}

	/** Writes every pending value in bind order and returns the ids that were written. */
	@SuppressWarnings("unchecked")
	public List<String> apply() {
		List<String> applied = new ArrayList<>();
		for (Map.Entry<String, Binding<?>> e : bindings.entrySet()) {
			if (pending.containsKey(e.getKey())) {
				((Binding<Object>) e.getValue()).write().accept(pending.get(e.getKey()));
				applied.add(e.getKey());
			}
		}
		pending.clear();
		return applied;
	}

	private Binding<?> binding(String id) {
		Binding<?> b = bindings.get(id);
		if (b == null) {
			throw new IllegalArgumentException("Unknown setting id " + id);
		}
		return b;
	}
}
