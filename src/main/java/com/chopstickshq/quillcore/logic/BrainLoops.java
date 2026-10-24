package com.chopstickshq.quillcore.logic;

/** Switch for the flattened Brain behaviour walk. Set -Dquillcore.vanillaBrainLoops=true to use vanilla's loops. */
public final class BrainLoops {
	public static volatile boolean forceVanilla = Boolean.getBoolean("quillcore.vanillaBrainLoops");

	private BrainLoops() {}
}
