package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.render.BlockEntityRenderCutoff;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Cancels render for distant allowlisted static block entities (signs, skulls).
 * Uses camera position from the dispatcher. Never touches chests/furnaces/spawners.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderCutoffMixin {
	@Shadow
	public net.minecraft.client.Camera camera;

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private <E extends BlockEntity> void quillcore$beCutoff(
			E blockEntity,
			float tickDelta,
			PoseStack poseStack,
			MultiBufferSource bufferSource,
			CallbackInfo ci
	) {
		if (camera == null) {
			return;
		}
		String typeId = typeIdOf(blockEntity);
		if (typeId == null) {
			return;
		}
		QuillConfig cfg = QuillConfig.get();
		Vec3 cam = camera.getPosition();
		BlockPos pos = blockEntity.getBlockPos();
		double dx = pos.getX() + 0.5 - cam.x;
		double dy = pos.getY() + 0.5 - cam.y;
		double dz = pos.getZ() + 0.5 - cam.z;
		double distSq = dx * dx + dy * dy + dz * dz;
		if (BlockEntityRenderCutoff.shouldSkipRender(
				cfg.masterEnabled,
				cfg.renderEnabled,
				cfg.blockEntityRenderCutoffEnabled,
				cfg.blockEntityRenderCutoffBlocks,
				distSq,
				typeId
		)) {
			ci.cancel();
		}
	}

	private static String typeIdOf(BlockEntity be) {
		if (be instanceof HangingSignBlockEntity) {
			return "hanging_sign";
		}
		if (be instanceof SignBlockEntity) {
			return "sign";
		}
		if (be instanceof SkullBlockEntity) {
			return "skull";
		}
		return null;
	}
}
