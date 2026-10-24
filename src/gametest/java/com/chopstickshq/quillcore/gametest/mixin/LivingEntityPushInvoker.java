package com.chopstickshq.quillcore.gametest.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityPushInvoker {
	@Invoker("pushEntities")
	void quillcore$pushEntities();
}
