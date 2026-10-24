package com.chopstickshq.quillcore.memory.mixin;

import com.chopstickshq.quillcore.memory.ShapeInterner;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase$Cache")
public abstract class BlockStateCacheMixin {
	@Shadow @Final @Mutable protected VoxelShape collisionShape;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void quillcore$share(CallbackInfo ci) {
		collisionShape = ShapeInterner.intern(collisionShape);
	}
}
