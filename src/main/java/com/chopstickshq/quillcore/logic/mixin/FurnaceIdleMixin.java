package com.chopstickshq.quillcore.logic.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * An unlit furnace with no progress and nothing to start with does nothing in vanilla's tick
 * except build a recipe input. Return before that.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceIdleMixin {
	@Shadow int litTimeRemaining;
	@Shadow int cookingTimer;
	@Shadow protected NonNullList<ItemStack> items;

	@Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
	private static void quillcore$idle(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity be, CallbackInfo ci) {
		FurnaceIdleMixin f = (FurnaceIdleMixin) (Object) be;
		if (f.litTimeRemaining <= 0 && f.cookingTimer <= 0 && (f.items.get(0).isEmpty() || f.items.get(1).isEmpty())) ci.cancel();
	}
}
