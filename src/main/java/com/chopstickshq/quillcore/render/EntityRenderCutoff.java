package com.chopstickshq.quillcore.render;

/**
 * Pure decision logic for client entity render-distance cutoff.
 * No Minecraft imports — unit-testable on a headless JVM.
 *
 * Semantics: when enabled, skip rendering entities whose squared distance from the
 * camera exceeds the cutoff, except the local player's vehicle/passengers and the
 * camera entity itself. Purely visual; never touches simulation.
 */
public final class EntityRenderCutoff {
	private EntityRenderCutoff() {}

	/**
	 * @return true if rendering should be skipped (early-exit shouldRender → false)
	 */
	public static boolean shouldSkipRender(
			boolean masterEnabled,
			boolean renderEnabled,
			boolean cutoffEnabled,
			double cutoffBlocks,
			double distSqFromCamera,
			boolean isCameraEntity,
			boolean isVehicleOrPassengerOfCamera
	) {
		if (!masterEnabled || !renderEnabled || !cutoffEnabled) {
			return false;
		}
		if (isCameraEntity || isVehicleOrPassengerOfCamera) {
			return false;
		}
		double cutoff = Math.max(0.0, cutoffBlocks);
		if (cutoff <= 0.0) {
			return false;
		}
		double cutoffSq = cutoff * cutoff;
		return distSqFromCamera > cutoffSq;
	}
}
