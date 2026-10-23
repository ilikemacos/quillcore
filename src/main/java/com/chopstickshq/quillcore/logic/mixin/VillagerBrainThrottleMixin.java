package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.logic.BrainIdleThrottle;
import com.chopstickshq.quillcore.logic.BrainIdleThrottleState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles {@link Brain#tick} for idle distant villagers.
 * Trading, panic, hurt, and near-player cases never skip. Profession/timer side
 * effects in {@code customServerAiStep} still run — only the Brain.tick call is gated.
 */
@Mixin(Villager.class)
public abstract class VillagerBrainThrottleMixin {
	@Inject(method = "customServerAiStep", at = @At("HEAD"))
	private void quillcore$beginBrainThrottle(ServerLevel serverLevel, CallbackInfo ci) {
		Villager self = (Villager) (Object) this;
		QuillConfig cfg = QuillConfig.get();
		Player nearest = self.level().getNearestPlayer(self, cfg.brainIdleThrottlePlayerRadiusBlocks);
		double distSq = nearest == null ? Double.POSITIVE_INFINITY : nearest.distanceToSqr(self);
		boolean panicking = self.getBrain().isActive(Activity.PANIC) || self.getBrain().isActive(Activity.HIDE);
		boolean skip = BrainIdleThrottle.shouldSkipBrainTick(
				cfg.masterEnabled,
				cfg.logicEnabled,
				cfg.brainIdleThrottleEnabled,
				cfg.brainIdleThrottleMaxSkipTicks,
				cfg.brainIdleThrottlePlayerRadiusBlocks,
				distSq,
				self.isTrading(),
				self.isAlive(),
				self.hurtTime,
				panicking,
				BrainIdleThrottleState.consecutiveSkips(self.getId())
		);
		BrainIdleThrottleState.beginTick(skip);
	}

	@Inject(method = "customServerAiStep", at = @At("RETURN"))
	private void quillcore$endBrainThrottle(ServerLevel serverLevel, CallbackInfo ci) {
		Villager self = (Villager) (Object) this;
		boolean skipped = BrainIdleThrottleState.shouldSkipBrain();
		BrainIdleThrottleState.endTick(self.getId(), skipped);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Redirect(
			method = "customServerAiStep",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/ai/Brain;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"
			)
	)
	private void quillcore$maybeSkipBrainTick(Brain brain, ServerLevel level, LivingEntity entity) {
		if (!BrainIdleThrottleState.shouldSkipBrain()) {
			brain.tick(level, entity);
		}
	}
}
