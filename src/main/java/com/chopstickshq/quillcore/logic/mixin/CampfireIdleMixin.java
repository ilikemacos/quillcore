package com.chopstickshq.quillcore.logic.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A lit campfire with nothing on it skips the slot loop. */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireIdleMixin {
	@Shadow @Final private NonNullList<ItemStack> items;

	@Inject(method = "cookTick", at = @At("HEAD"), cancellable = true)
	private static void quillcore$empty(ServerLevel level, BlockPos pos, BlockState state, CampfireBlockEntity be,
			RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> check, CallbackInfo ci) {
		for (ItemStack s : ((CampfireIdleMixin) (Object) be).items) if (!s.isEmpty()) return;
		ci.cancel();
	}
}
