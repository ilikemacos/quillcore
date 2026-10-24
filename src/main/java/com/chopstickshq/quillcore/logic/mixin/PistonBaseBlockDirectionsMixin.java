package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.Directions;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Read-only loops over every direction; reuse one array instead of cloning it each call. */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockDirectionsMixin {
	@Redirect(method = {"getNeighborSignal"}, require = 2, allow = 2,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;values()[Lnet/minecraft/core/Direction;"))
	private static Direction[] quillcore$sharedDirections() {
		return Directions.all();
	}
}
