package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.Directions;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Read-only loops over every direction; reuse one array instead of cloning it each call. */
@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverDirectionsMixin {
	@Redirect(method = {"addBranchingBlocks"}, require = 1, allow = 1,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;values()[Lnet/minecraft/core/Direction;"))
	private static Direction[] quillcore$sharedDirections() {
		return Directions.all();
	}
}
