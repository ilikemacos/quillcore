package com.chopstickshq.quillcore.memory;

/** Pool for baked quad vertex arrays. Lives only while models bake. */
public final class QuadData {
	public static final IntArrayInterner VERTICES = new IntArrayInterner();

	private QuadData() {
	}
}
