package com.chopstickshq.quillcore.logic;

import java.util.Optional;

/** Implemented on SectionStorage by a mixin. */
public interface PoiColumnsHolder {
	/** Null when the world is too tall to track. */
	PoiColumns quillcore$columns();

	Optional<?> quillcore$stored(long section);

	int quillcore$minSection();
}
