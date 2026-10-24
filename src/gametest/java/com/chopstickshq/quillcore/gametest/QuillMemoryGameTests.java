package com.chopstickshq.quillcore.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;

/** Checks the memory patches give the same answers vanilla would. */
public class QuillMemoryGameTests {
	/**
	 * For every state of every block, setValue must land on the state whose property map is the
	 * old one with that single value swapped. That is exactly what vanilla's neighbour map holds.
	 */
	@GameTest
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setValueMatchesPropertyMaps(GameTestHelper h) {
		int checked = 0;
		for (Block block : BuiltInRegistries.BLOCK) {
			Map<Map<Property<?>, Comparable<?>>, BlockState> byValues = new HashMap<>();
			for (BlockState s : block.getStateDefinition().getPossibleStates()) {
				byValues.put(new HashMap<>(s.getValues()), s);
			}
			for (BlockState s : block.getStateDefinition().getPossibleStates()) {
				for (Property p : s.getProperties()) {
					for (Object v : p.getPossibleValues()) {
						Map<Property<?>, Comparable<?>> want = new HashMap<>(s.getValues());
						want.put(p, (Comparable<?>) v);
						BlockState got = s.setValue(p, (Comparable) v);
						if (got != byValues.get(want)) {
							h.fail(Component.literal(s + " with " + p.getName() + "=" + v + " gave " + got));
						}
						checked++;
					}
				}
			}
		}
		if (checked < 100_000) {
			h.fail(Component.literal("only " + checked + " lookups checked"));
		}
		h.succeed();
	}

	@GameTest
	public void vanillaNeighbourMapIsGone(GameTestHelper h) throws ReflectiveOperationException {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ferritecore")) {
			h.succeed(); // our memory mixins step aside for FerriteCore
			return;
		}
		java.lang.reflect.Field f = net.minecraft.world.level.block.state.StateHolder.class.getDeclaredField("neighbours");
		f.setAccessible(true);
		if (f.get(net.minecraft.world.level.block.Blocks.REDSTONE_WIRE.defaultBlockState()) != null) {
			h.fail(Component.literal("neighbour map still built"));
		}
		h.succeed();
	}

	@GameTest
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void badValuesStillThrow(GameTestHelper h) {
		BlockState s = net.minecraft.world.level.block.Blocks.REDSTONE_WIRE.defaultBlockState();
		Property power = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER;
		try {
			s.setValue(power, 99);
			h.fail(Component.literal("power=99 accepted"));
		} catch (IllegalArgumentException expected) {
		}
		try {
			net.minecraft.world.level.block.Blocks.STONE.defaultBlockState().setValue(power, 1);
			h.fail(Component.literal("missing property accepted"));
		} catch (IllegalArgumentException expected) {
		}
		h.succeed();
	}

	/** Shared collision shapes must cover exactly what the block computes fresh, for every state. */
	@GameTest
	public void sharedShapesMatchFreshOnes(GameTestHelper h) throws ReflectiveOperationException {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ferritecore")) {
			h.succeed(); // our memory mixins step aside for FerriteCore
			return;
		}
		// protected in vanilla; dev runtime keeps mojang names
		java.lang.reflect.Method compute = net.minecraft.world.level.block.state.BlockBehaviour.class.getDeclaredMethod("getCollisionShape",
				BlockState.class, net.minecraft.world.level.BlockGetter.class, net.minecraft.core.BlockPos.class, net.minecraft.world.phys.shapes.CollisionContext.class);
		compute.setAccessible(true);
		int checked = 0;
		for (Block block : BuiltInRegistries.BLOCK) {
			for (BlockState s : block.getStateDefinition().getPossibleStates()) {
				net.minecraft.world.phys.shapes.VoxelShape cached = s.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO);
				net.minecraft.world.phys.shapes.VoxelShape fresh = (net.minecraft.world.phys.shapes.VoxelShape) compute.invoke(s.getBlock(), s,
						net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO, net.minecraft.world.phys.shapes.CollisionContext.empty());
				if (net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(cached, fresh, net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME)) {
					h.fail(Component.literal("collision shape differs for " + s));
				}
				checked++;
			}
		}
		h.assertTrue(checked > 10_000, Component.literal("only checked " + checked));
		{
			h.assertTrue(com.chopstickshq.quillcore.memory.ShapeInterner.unique() < com.chopstickshq.quillcore.memory.ShapeInterner.seen() / 4,
					Component.literal("shapes not shared: " + com.chopstickshq.quillcore.memory.ShapeInterner.unique() + " of " + com.chopstickshq.quillcore.memory.ShapeInterner.seen()));
		}
		h.succeed();
	}
}
