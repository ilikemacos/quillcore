package com.chopstickshq.quillcore.logic;

import net.minecraft.core.Direction;

/** One shared copy of {@code Direction.values()} for loops that only read it. */
public final class Directions {
	public static final Direction[] ALL = Direction.values();

	private Directions() {
	}

	public static Direction[] all() {
		return ALL;
	}
}
