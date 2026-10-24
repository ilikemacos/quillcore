package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.CollisionKindHolder;
import com.chopstickshq.quillcore.logic.CollisionKinds;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateCollisionKindMixin implements CollisionKindHolder {
	@Unique
	private byte quillcore$kind = CollisionKinds.UNKNOWN;

	@Override
	public byte quillcore$collisionKind() {
		byte k = quillcore$kind;
		if (k == CollisionKinds.UNKNOWN) quillcore$kind = k = CollisionKinds.compute((BlockState) (Object) this);
		return k;
	}
}
