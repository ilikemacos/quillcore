package com.chopstickshq.quillcore.logic;

import com.chopstickshq.quillcore.logic.mixin.BlockBehaviourAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sorts block states by how their collision shape can be known ahead of time. A state is only {@link #EMPTY} or
 * {@link #FULL} when its block keeps the default collision method (so the entity's context can't change it), has no
 * dynamic shape, and (for a full cube) no offset. Anything else, or anything we can't check, is {@link #SLOW} and gets the vanilla lookup.
 * Scaffolding, powder snow, moving pistons and bamboo all land on SLOW.
 */
public final class CollisionKinds {
	public static final byte UNKNOWN = 0;
	public static final byte SLOW = 1;
	public static final byte EMPTY = 2;
	public static final byte FULL = 3;

	/** Send every walk to vanilla. Set by -Dquillcore.vanillaBlockCollisions=true, and by the equivalence test. */
	public static volatile boolean forceVanilla = Boolean.getBoolean("quillcore.vanillaBlockCollisions");

	private static final String NAME = resolve();

	private static final ClassValue<Boolean> OVERRIDES = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> c) {
			if (NAME == null) return true;
			for (Class<?> k = c; k != null && k != BlockBehaviour.class; k = k.getSuperclass()) {
				try {
					k.getDeclaredMethod(NAME, BlockState.class, BlockGetter.class, BlockPos.class, CollisionContext.class);
					return true;
				} catch (NoSuchMethodException ignored) {
				} catch (Throwable t) {
					return true;
				}
			}
			return false;
		}
	};

	private CollisionKinds() {
	}

	private static String resolve() {
		try {
			String n = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_4970",
					"method_9549", "(Lnet/minecraft/class_2680;Lnet/minecraft/class_1922;Lnet/minecraft/class_2338;Lnet/minecraft/class_3726;)Lnet/minecraft/class_265;");
			BlockBehaviour.class.getDeclaredMethod(n, BlockState.class, BlockGetter.class, BlockPos.class, CollisionContext.class);
			return n;
		} catch (Throwable t) {
			return null;
		}
	}

	public static byte compute(BlockState state) {
		try {
			Block block = state.getBlock();
			if (OVERRIDES.get(block.getClass()) || ((BlockBehaviourAccessor) block).quillcore$dynamicShape()) {
				return SLOW;
			}
			VoxelShape s = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
			// An offset moves the shape by position, which can't change an empty shape (vanilla's own cache relies on this).
			if (s.isEmpty()) return EMPTY;
			if (state.hasOffsetFunction()) return SLOW;
			if (s == Shapes.block()) return FULL;
			return SLOW;
		} catch (Throwable t) {
			return SLOW;
		}
	}
}
