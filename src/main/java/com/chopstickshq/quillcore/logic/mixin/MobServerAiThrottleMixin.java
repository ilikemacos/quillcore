package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.logic.IdleAiThrottle;
import com.chopstickshq.quillcore.logic.IdleAiThrottleState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles GoalSelector / targetSelector ticks for idle distant mobs.
 * Sensing, navigation, and customServerAiStep always run (behavior-preserving intent).
 * Mixin wiring is compile-verified; in-game verification still required on a client.
 */
@Mixin(Mob.class)
public abstract class MobServerAiThrottleMixin {
	@Inject(method = "serverAiStep", at = @At("HEAD"))
	private void quillcore$beginThrottle(CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		boolean skip = false;
		if (self.level() instanceof ServerLevel) {
			QuillConfig cfg = QuillConfig.get();
			LivingEntity target = self.getTarget();
			Player nearest = self.level().getNearestPlayer(self, cfg.idleAiThrottlePlayerRadiusBlocks);
			double distSq = nearest == null
					? Double.POSITIVE_INFINITY
					: nearest.distanceToSqr(self);
			skip = IdleAiThrottle.shouldSkipGoalTick(
					cfg.masterEnabled,
					cfg.logicEnabled,
					cfg.idleAiThrottleEnabled,
					cfg.idleAiThrottleMaxSkipTicks,
					cfg.idleAiThrottlePlayerRadiusBlocks,
					distSq,
					target != null && target.isAlive(),
					self.isAlive(),
					self.hurtTime,
					IdleAiThrottleState.consecutiveSkips(self.getId())
			);
		}
		IdleAiThrottleState.beginTick(skip);
	}

	@Inject(method = "serverAiStep", at = @At("RETURN"))
	private void quillcore$endThrottle(CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		boolean skipped = IdleAiThrottleState.shouldSkipSelectors();
		IdleAiThrottleState.endTick(self.getId(), skipped);
	}

	@Redirect(
			method = "serverAiStep",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;tick()V")
	)
	private void quillcore$maybeSkipTick(GoalSelector selector) {
		if (!IdleAiThrottleState.shouldSkipSelectors()) {
			selector.tick();
		}
	}

	@Redirect(
			method = "serverAiStep",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;tickRunningGoals(Z)V")
	)
	private void quillcore$maybeSkipRunning(GoalSelector selector, boolean tickAllRunning) {
		if (!IdleAiThrottleState.shouldSkipSelectors()) {
			selector.tickRunningGoals(tickAllRunning);
		}
	}
}
