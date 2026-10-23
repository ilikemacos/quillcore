package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.render.ParticleRenderCutoff;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Skips spawning particles beyond the configured distance from the camera.
 * Visual only, particles are client side.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleRenderCutoffMixin {
	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void quillcore$particleCutoff(
			ParticleOptions options,
			double x,
			double y,
			double z,
			double xSpeed,
			double ySpeed,
			double zSpeed,
			CallbackInfoReturnable<Particle> cir
	) {
		QuillConfig cfg = QuillConfig.get();
		Entity camera = Minecraft.getInstance().getCameraEntity();
		if (camera == null) {
			return;
		}
		double dx = x - camera.getX();
		double dy = y - camera.getY();
		double dz = z - camera.getZ();
		double distSq = dx * dx + dy * dy + dz * dz;
		if (ParticleRenderCutoff.shouldSkipSpawn(
				cfg.masterEnabled,
				cfg.renderEnabled,
				cfg.particleRenderCutoffEnabled,
				cfg.particleRenderCutoffBlocks,
				distSq
		)) {
			cir.setReturnValue(null);
		}
	}
}
