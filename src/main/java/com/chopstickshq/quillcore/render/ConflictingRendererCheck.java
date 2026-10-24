package com.chopstickshq.quillcore.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Finds loaded mods that replace the chunk renderer. fabric.mod.json already lists
 * them under "breaks", this catches dev setups that skip that check.
 */
public final class ConflictingRendererCheck {
	private ConflictingRendererCheck() {}

	/** Same list as "breaks" in fabric.mod.json. */
	public static final List<String> CONFLICTING_MOD_IDS = List.of("sodium");

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
