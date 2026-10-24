package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.CollisionKindHolder;
import com.chopstickshq.quillcore.logic.CollisionKinds;
import com.google.common.collect.AbstractIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;

/**
 * The same walk as vanilla {@code computeNext}, in the same cursor order and with the same chunk lookup (so unloaded
 * chunks and world height behave the same), with two shortcuts: a position inside an all-air section is skipped, since
 * vanilla would read air there and find nothing, and a state whose collision shape is known to be empty or a full cube
 * (see {@link CollisionKinds}) skips the shape lookup. Only plain entity contexts take this path; minecarts and other
 * custom contexts go to vanilla. The world border isn't part of this iterator and is untouched.
 */
@Mixin(BlockCollisions.class)
public abstract class BlockCollisionsFastMixin<T> extends AbstractIterator<T> {
	@Shadow @Final private AABB box;
	@Shadow @Final private CollisionContext context;
	@Shadow @Final private Cursor3D cursor;
	@Shadow @Final private BlockPos.MutableBlockPos pos;
	@Shadow @Final private VoxelShape entityShape;
	@Shadow @Final private CollisionGetter collisionGetter;
	@Shadow @Final private boolean onlySuffocatingBlocks;
	@Shadow @Final private BiFunction<BlockPos.MutableBlockPos, VoxelShape, T> resultProvider;

	@Shadow
	protected abstract BlockGetter getChunk(int x, int z);

	@Inject(method = "computeNext", at = @At("HEAD"), cancellable = true)
	private void quillcore$fastNext(CallbackInfoReturnable<T> cir) {
		if (CollisionKinds.forceVanilla || context.getClass() != EntityCollisionContext.class) return;
		while (cursor.advance()) {
			int x = cursor.nextX();
			int y = cursor.nextY();
			int z = cursor.nextZ();
			int type = cursor.getNextType();
			if (type == 3) continue;
			BlockGetter getter = getChunk(x, z);
			if (getter == null) continue;
			if (getter instanceof LevelChunk chunk && !chunk.getLevel().isDebug()) {
				int idx = chunk.getSectionIndex(y);
				if (idx >= 0 && idx < chunk.getSections().length && chunk.getSection(idx).hasOnlyAir()) continue;
			}
			pos.set(x, y, z);
			BlockState state = getter.getBlockState(pos);
			if (onlySuffocatingBlocks && !state.isSuffocating(getter, pos)) continue;
			if (type == 1 && !state.hasLargeCollisionShape()) continue;
			if (type == 2 && !state.is(Blocks.MOVING_PISTON)) continue;
			byte kind = ((CollisionKindHolder) state).quillcore$collisionKind();
			if (kind == CollisionKinds.EMPTY) continue;
			VoxelShape shape = kind == CollisionKinds.FULL ? Shapes.block() : context.getCollisionShape(state, collisionGetter, pos);
			if (shape == Shapes.block()) {
				if (box.intersects(x, y, z, x + 1.0, y + 1.0, z + 1.0)) {
					cir.setReturnValue(resultProvider.apply(pos, shape.move(pos)));
					return;
				}
			} else {
				VoxelShape moved = shape.move(pos);
				if (!moved.isEmpty() && Shapes.joinIsNotEmpty(moved, entityShape, BooleanOp.AND)) {
					cir.setReturnValue(resultProvider.apply(pos, moved));
					return;
				}
			}
		}
		cir.setReturnValue(endOfData());
	}
}
