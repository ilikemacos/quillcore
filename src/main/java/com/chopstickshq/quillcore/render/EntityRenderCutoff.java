package com.chopstickshq.quillcore.render;

/**
 * Skips drawing entities past the cutoff distance from the camera. The camera
 * entity and the player's vehicle and passengers are always drawn.
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
