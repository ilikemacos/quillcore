package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.render.EntityRenderCutoff;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Early-exit for {@link EntityRenderDispatcher#shouldRender} when an entity is
 * beyond the configured render cutoff. Default cutoff is generous (128 blocks).
 * Camera entity and its vehicle/passengers are never culled.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderCutoffMixin {
	@Inject(
			method = "shouldRender",
			at = @At("HEAD"),
			cancellable = true
	)
	private <E extends Entity> void quillcore$entityCutoff(
			E entity,
			Frustum frustum,
			double camX,
			double camY,
			double camZ,
			CallbackInfoReturnable<Boolean> cir
	) {
		QuillConfig cfg = QuillConfig.get();
		Entity camera = Minecraft.getInstance().getCameraEntity();
		boolean isCamera = camera != null && entity == camera;
		boolean related = false;
		if (camera != null && !isCamera) {
			related = entity.isPassengerOfSameVehicle(camera)
					|| camera.getVehicle() == entity
					|| entity.getVehicle() == camera;
		}
		double dx = entity.getX() - camX;
		double dy = entity.getY() - camY;
		double dz = entity.getZ() - camZ;
		double distSq = dx * dx + dy * dy + dz * dz;
		if (EntityRenderCutoff.shouldSkipRender(
				cfg.masterEnabled,
				cfg.renderEnabled,
				cfg.entityRenderCutoffEnabled,
				cfg.entityRenderCutoffBlocks,
				distSq,
				isCamera,
				related
		)) {
			cir.setReturnValue(false);
		}
	}
}
