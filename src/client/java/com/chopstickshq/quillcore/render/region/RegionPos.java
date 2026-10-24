package com.chopstickshq.quillcore.render.region;

/** Sections are grouped 8 x 4 x 8 into one region, one buffer and one multidraw per pass. */
public final class RegionPos {
	public static final int W = 8, H = 4, D = 8;

	private RegionPos() {}

	public static long of(int sx, int sy, int sz) {
		return pack(Math.floorDiv(sx, W), Math.floorDiv(sy, H), Math.floorDiv(sz, D));
	}

	/** Slot of a section inside its region, 0..255. */
	public static int slot(int sx, int sy, int sz) {
		return (Math.floorMod(sy, H) * D + Math.floorMod(sz, D)) * W + Math.floorMod(sx, W);
	}

	static long pack(int rx, int ry, int rz) {
		return ((long) rx & 0x3FFFFF) << 42 | ((long) rz & 0x3FFFFF) << 20 | (ry & 0xFFFFF);
	}

	public static int x(long r) {
		return (int) (r >> 42);
	}

	public static int z(long r) {
		return (int) (r << 22 >> 42);
	}

	public static int y(long r) {
		return (int) (r << 44 >> 44);
	}
}
