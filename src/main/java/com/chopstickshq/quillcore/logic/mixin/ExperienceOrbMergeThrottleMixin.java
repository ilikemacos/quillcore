package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.logic.ItemMergeThrottle;
import com.chopstickshq.quillcore.logic.ItemMergeThrottleState;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles {@code ExperienceOrb#scanForMerges} when far from players.
 * Follow-player / pickup / age untouched.
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMergeThrottleMixin {
	@Inject(method = "scanForMerges", at = @At("HEAD"), cancellable = true)
	private void quillcore$throttleXpMerge(CallbackInfo ci) {
		ExperienceOrb self = (ExperienceOrb) (Object) this;
		if (self.level().isClientSide) {
			return;
		}
		QuillConfig cfg = QuillConfig.get();
		Player nearest = self.level().getNearestPlayer(self, cfg.itemMergeThrottlePlayerRadiusBlocks);
		double distSq = nearest == null ? Double.POSITIVE_INFINITY : nearest.distanceToSqr(self);
		boolean skip = ItemMergeThrottle.shouldSkipMergeScan(
				cfg.masterEnabled,
				cfg.logicEnabled,
				cfg.itemMergeThrottleEnabled,
				cfg.itemMergeThrottleMaxSkipTicks,
				cfg.itemMergeThrottlePlayerRadiusBlocks,
				distSq,
				self.isAlive(),
				ItemMergeThrottleState.consecutiveSkips(self.getId())
		);
		ItemMergeThrottleState.record(self.getId(), skip);
		if (skip) {
			ci.cancel();
		}
	}
}
