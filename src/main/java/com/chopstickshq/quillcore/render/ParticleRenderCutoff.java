package com.chopstickshq.quillcore.render;

/**
 * Pure decision logic for client particle spawn-distance cutoff.
 * Particles are client-visual only; skipping distant spawns never changes simulation.
 */
public final class ParticleRenderCutoff {
	private ParticleRenderCutoff() {}

	/** @return true if createParticle should return null (skip spawn) */
	public static boolean shouldSkipSpawn(
			boolean masterEnabled,
			boolean renderEnabled,
			boolean cutoffEnabled,
			double cutoffBlocks,
			double distSqFromCamera
	) {
		if (!masterEnabled || !renderEnabled || !cutoffEnabled) {
			return false;
		}
		double cutoff = Math.max(0.0, cutoffBlocks);
		if (cutoff <= 0.0) {
			return false;
		}
		return distSqFromCamera > cutoff * cutoff;
	}
}
