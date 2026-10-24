package com.chopstickshq.quillcore.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Pure detection of chunk renderers that QuillCore replaces and cannot run alongside. */
public final class ConflictingRendererCheck {
	private ConflictingRendererCheck() {}

	/** Mod ids declared in fabric.mod.json "breaks". Keep in sync. */
	public static final List<String> CONFLICTING_MOD_IDS = List.of("sodium");

	/**
	 * @param isModLoaded loader query, e.g. {@code FabricLoader.getInstance()::isModLoaded}
	 * @return conflicting mod ids that are loaded, in declaration order (empty if none)
	 */
	public static List<String> findLoaded(Predicate<String> isModLoaded) {
		List<String> out = new ArrayList<>();
		if (isModLoaded == null) {
			return out;
		}
		for (String id : CONFLICTING_MOD_IDS) {
			if (isModLoaded.test(id)) {
				out.add(id);
			}
		}
		return out;
	}

	public static boolean hasConflict(Predicate<String> isModLoaded) {
		return !findLoaded(isModLoaded).isEmpty();
	}
}
