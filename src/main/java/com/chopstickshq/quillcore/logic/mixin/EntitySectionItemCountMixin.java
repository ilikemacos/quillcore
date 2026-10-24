package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.ItemCount;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySection.class)
public abstract class EntitySectionItemCountMixin<T extends EntityAccess> implements ItemCount {
	@Unique
	private int quillcore$items;

	@Override
	public int quillcore$items() {
		return quillcore$items;
	}

	@Inject(method = "add", at = @At("TAIL"))
	private void quillcore$added(T entity, CallbackInfo ci) {
		if (entity instanceof ItemEntity) quillcore$items++;
	}

	@Inject(method = "remove", at = @At("RETURN"))
	private void quillcore$removed(T entity, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && entity instanceof ItemEntity) quillcore$items--;
	}
}
