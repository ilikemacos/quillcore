package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.HardColliderCount;
import com.chopstickshq.quillcore.logic.HardColliders;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts the section's entities that may block movement, kept in step with add and remove. */
@Mixin(EntitySection.class)
public abstract class EntitySectionCollidersMixin<T extends EntityAccess> implements HardColliderCount {
	@Unique
	private int quillcore$hard;

	@Override
	public int quillcore$hardColliders() {
		return quillcore$hard;
	}

	@Inject(method = "add", at = @At("TAIL"))
	private void quillcore$added(T entity, CallbackInfo ci) {
		if (HardColliders.mayCollide(entity)) quillcore$hard++;
	}

	@Inject(method = "remove", at = @At("RETURN"))
	private void quillcore$removed(T entity, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && HardColliders.mayCollide(entity)) quillcore$hard--;
	}
}
