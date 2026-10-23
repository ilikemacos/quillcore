package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.logic.PathNavThrottle;
import com.chopstickshq.quillcore.logic.PathNavThrottleState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips {@link PathNavigation#tick} for far idle mobs with no active path.
 * Active path following is never interrupted.
 */
@Mixin(PathNavigation.class)
public abstract class PathNavigationThrottleMixin {
	@Shadow @Final protected Mob mob;

	@Shadow public abstract boolean isDone();

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void quillcore$throttleNav(CallbackInfo ci) {
		if (this.mob.level().isClientSide) {
			return;
		}
		QuillConfig cfg = QuillConfig.get();
		LivingEntity target = this.mob.getTarget();
		Player nearest = this.mob.level().getNearestPlayer(this.mob, cfg.pathNavThrottlePlayerRadiusBlocks);
		double distSq = nearest == null ? Double.POSITIVE_INFINITY : nearest.distanceToSqr(this.mob);
		boolean skip = PathNavThrottle.shouldSkipNavTick(
				cfg.masterEnabled,
				cfg.logicEnabled,
				cfg.pathNavThrottleEnabled,
				cfg.pathNavThrottleMaxSkipTicks,
				cfg.pathNavThrottlePlayerRadiusBlocks,
				distSq,
				this.isDone(),
				target != null && target.isAlive(),
				this.mob.isAlive(),
				this.mob.hurtTime,
				PathNavThrottleState.consecutiveSkips(this.mob.getId())
		);
		PathNavThrottleState.record(this.mob.getId(), skip);
		if (skip) {
			ci.cancel();
		}
	}
}
