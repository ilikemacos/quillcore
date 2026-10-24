package com.chopstickshq.quillcore.memory;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Hands out one instance per distinct voxel shape. Two shapes match when their grid coords and
 * merged boxes match, which pins down the same filled cells.
 */
public final class ShapeInterner {
	private record Key(List<Double> x, List<Double> y, List<Double> z, List<AABB> boxes) {}

	private static final ConcurrentHashMap<Key, VoxelShape> POOL = new ConcurrentHashMap<>();
	private static final LongAdder SEEN = new LongAdder();

	private ShapeInterner() {}

	public static VoxelShape intern(VoxelShape s) {
		if (s == null) return null;
		SEEN.increment();
		Key k = new Key(List.copyOf(s.getCoords(Direction.Axis.X)), List.copyOf(s.getCoords(Direction.Axis.Y)),
				List.copyOf(s.getCoords(Direction.Axis.Z)), List.copyOf(s.toAabbs()));
		VoxelShape had = POOL.putIfAbsent(k, s);
		return had == null ? s : had;
	}

	public static long seen() {
		return SEEN.sum();
	}

	public static int unique() {
		return POOL.size();
	}
}
