package com.chopstickshq.quillcore.gametest.mixin;

import com.chopstickshq.quillcore.gametest.QuillAutoBench;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test-mod only: while the autobench runs, ignore AFK / minimized / focus-loss frame-rate throttling. */
@Mixin(FramerateLimitTracker.class)
public class FramerateLimitTrackerMixin {
	@Inject(method = "getThrottleReason", at = @At("HEAD"), cancellable = true)
	private void quilltest$noThrottleDuringBench(CallbackInfoReturnable<FramerateLimitTracker.FramerateThrottleReason> cir) {
		if (QuillAutoBench.ACTIVE) cir.setReturnValue(FramerateLimitTracker.FramerateThrottleReason.NONE);
	}
}
