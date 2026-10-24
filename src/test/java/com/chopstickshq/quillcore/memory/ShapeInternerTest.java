package com.chopstickshq.quillcore.memory;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShapeInternerTest {
	@Test
	void sameBoxSharesInstance() {
		VoxelShape a = ShapeInterner.intern(Shapes.box(0, 0, 0, 1, 0.5, 1));
		VoxelShape b = ShapeInterner.intern(Shapes.box(0, 0, 0, 1, 0.5, 1));
		assertSame(a, b);
	}

	@Test
	void differentBoxesStayApart() {
		assertNotSame(ShapeInterner.intern(Shapes.box(0, 0, 0, 1, 0.5, 1)), ShapeInterner.intern(Shapes.box(0, 0.5, 0, 1, 1, 1)));
	}

	@Test
	void sameCoordsDifferentCellsStayApart() {
		VoxelShape l = Shapes.or(Shapes.box(0, 0, 0, 0.5, 0.5, 0.5), Shapes.box(0.5, 0.5, 0.5, 1, 1, 1));
		VoxelShape r = Shapes.or(Shapes.box(0.5, 0, 0, 1, 0.5, 0.5), Shapes.box(0, 0.5, 0.5, 0.5, 1, 1));
		assertNotSame(ShapeInterner.intern(l), ShapeInterner.intern(r));
	}

	@Test
	void nullPassesThrough() {
		assertNull(ShapeInterner.intern(null));
	}
}
