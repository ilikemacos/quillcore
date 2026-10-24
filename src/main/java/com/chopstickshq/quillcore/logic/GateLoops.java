package com.chopstickshq.quillcore.logic;

/** Test hook for GateBehaviorLoopsMixin: true sends every gate back to vanilla's streams. */
public final class GateLoops {
	public static volatile boolean forceVanilla;

	private GateLoops() {}
}
