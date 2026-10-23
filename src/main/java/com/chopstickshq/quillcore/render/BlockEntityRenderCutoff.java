package com.chopstickshq.quillcore.render;

/**
 * Pure decision logic for distant static block-entity render skip.
 * Allowlist is encoded as string ids so unit tests need no MC classpath.
 *
 * Conservative: only types that are typically static decoration (signs, skulls).
 * Animated / interactive BEs (chests, furnaces, hoppers, spawners, etc.) are never skipped.
 */
public final class BlockEntityRenderCutoff {
	private BlockEntityRenderCutoff() {}

	public static boolean isAllowlistedType(String typeId) {
		if (typeId == null) {
			return false;
		}
		return switch (typeId) {
			case "sign", "hanging_sign", "skull" -> true;
			default -> false;
		};
	}

	/**
	 * @return true if BE rendering should be cancelled
	 */
	public static boolean shouldSkipRender(
			boolean masterEnabled,
			boolean renderEnabled,
			boolean cutoffEnabled,
			double cutoffBlocks,
			double distSqFromCamera,
			String typeId
	) {
		if (!masterEnabled || !renderEnabled || !cutoffEnabled) {
			return false;
		}
		if (!isAllowlistedType(typeId)) {
			return false;
		}
		double cutoff = Math.max(0.0, cutoffBlocks);
		if (cutoff <= 0.0) {
			return false;
		}
		return distSqFromCamera > cutoff * cutoff;
	}
}
