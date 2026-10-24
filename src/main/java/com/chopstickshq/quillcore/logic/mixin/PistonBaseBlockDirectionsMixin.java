package com.chopstickshq.quillcore.logic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.chopstickshq.quillcore.logic.Directions;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Read-only loops over every direction; reuse one array instead of cloning it each call. */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockDirectionsMixin {
	@WrapOperation(method = {"getNeighborSignal"}, require = 2, allow = 2,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;values()[Lnet/minecraft/core/Direction;"))
	private static Direction[] quillcore$sharedDirections(Operation<Direction[]> original) {
		return Directions.all();
	}
}
