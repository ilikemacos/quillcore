package com.chopstickshq.quillcore.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConflictingRendererCheckTest {
	@Test
	void noModsNoConflict() {
		assertFalse(ConflictingRendererCheck.hasConflict(id -> false));
		assertTrue(ConflictingRendererCheck.findLoaded(id -> false).isEmpty());
	}

	@Test
	void nullLoaderNoConflict() {
		assertFalse(ConflictingRendererCheck.hasConflict(null));
	}

	@Test
	void sodiumIsConflict() {
		assertTrue(ConflictingRendererCheck.hasConflict("sodium"::equals));
		assertEquals(List.of("sodium"), ConflictingRendererCheck.findLoaded("sodium"::equals));
	}

	@Test
	void unrelatedModsNoConflict() {
		assertFalse(ConflictingRendererCheck.hasConflict(id -> id.equals("lithium") || id.equals("modmenu")));
	}

	@Test
	void sodiumIsDeclared() {
		assertTrue(ConflictingRendererCheck.CONFLICTING_MOD_IDS.contains("sodium"));
	}
}
