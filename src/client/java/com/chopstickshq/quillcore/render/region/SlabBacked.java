package com.chopstickshq.quillcore.render.region;

/** Duck interface on {@code SectionBuffers}: non-null while the vertices live in a region slab. */
public interface SlabBacked {
	RegionSlabs.Range quillcore$range();

	void quillcore$setRange(RegionSlabs.Range range);

	static RegionSlabs.Range rangeOf(Object buffers) {
		return buffers instanceof SlabBacked s ? s.quillcore$range() : null;
	}
}
