package com.chopstickshq.quillcore.logic.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SecondaryPoiSensor;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Only farmers have secondary job blocks. Everyone else scans 225 blocks every sensor tick to
 * find nothing and then clears the memory, so clear it straight away.
 */
@Mixin(SecondaryPoiSensor.class)
public abstract class SecondaryPoiSensorMixin {
	@Inject(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;)V",
			at = @At("HEAD"), cancellable = true)
	private void quillcore$skipEmptyScan(ServerLevel level, Villager villager, CallbackInfo ci) {
		if (villager.getVillagerData().profession().value().secondaryPoi().isEmpty()) {
			villager.getBrain().eraseMemory(MemoryModuleType.SECONDARY_JOB_SITE);
			ci.cancel();
		}
	}
}
