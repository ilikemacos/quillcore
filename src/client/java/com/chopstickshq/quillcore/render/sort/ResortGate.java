package com.chopstickshq.quillcore.render.sort;

/**
 * Which translucent sections get re-sorted. Vanilla re-sorts every axis-aligned section on each block the camera
 * moves; past the nearby ring that churns worker tasks and index uploads for order changes nobody can see. Here those
 * wait until the camera enters another section. Nearby sections and point-of-view changes stay as vanilla.
 * Render thread only.
 */
public final class ResortGate {
	public static final boolean ENABLED = !Boolean.getBoolean("quillcore.vanillaResort");

	private static long cameraSection = Long.MIN_VALUE;
	private static boolean sectionChanged;
	private static long scheduled, skipped;

	private ResortGate() {
	}

	public static void beginFrame(double x, double y, double z) {
		long s = ((long) Math.floor(x) >> 4 & 0x3FFFFF) << 42 | ((long) Math.floor(z) >> 4 & 0x3FFFFF) << 20 | ((long) Math.floor(y) >> 4 & 0xFFFFF);
		sectionChanged = s != cameraSection;
		cameraSection = s;
	}

	/**
	 * @param moved the camera changed block this frame
	 * @param nearby the section is in the camera's nearby ring
	 * @param axisAligned the camera shares a section row, column or layer with it
	 * @param newPov the section's clamped direction to the camera differs from its last sort
	 */
	public static boolean shouldResort(boolean moved, boolean nearby, boolean axisAligned, boolean newPov) {
		boolean vanilla = newPov || moved && (axisAligned || nearby);
		if (!ENABLED) return vanilla;
		boolean ours = newPov || moved && nearby || moved && axisAligned && sectionChanged;
		if (vanilla && !ours) skipped++;
		return ours;
	}

	public static void scheduled() {
		scheduled++;
	}

	public static long scheduledCount() {
		return scheduled;
	}

	/** Resorts vanilla would have scheduled that the gate held back. */
	public static long skippedCount() {
		return skipped;
	}
}
