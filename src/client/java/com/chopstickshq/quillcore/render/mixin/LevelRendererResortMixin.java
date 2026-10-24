package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.sort.ResortGate;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Translucent re-sort scheduling through {@link ResortGate}. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererResortMixin {
	@Shadow
	private SectionRenderDispatcher sectionRenderDispatcher;

	@Inject(method = "scheduleTranslucentSectionResort", at = @At("HEAD"))
	private void quillcore$frame(Vec3 camera, CallbackInfo ci) {
		ResortGate.beginFrame(camera.x, camera.y, camera.z);
	}

	@Inject(method = "scheduleResort", at = @At("HEAD"), cancellable = true)
	private void quillcore$gate(SectionRenderDispatcher.RenderSection section, TranslucencyPointOfView pov, Vec3 camera, boolean moved, boolean nearby,
			CallbackInfo ci) {
		ci.cancel();
		pov.set(camera, section.getSectionNode());
		boolean newPov = section.getSectionMesh().isDifferentPointOfView(pov);
		if (!ResortGate.shouldResort(moved, nearby, pov.isAxisAligned(), newPov)) return;
		if (section.transparencyResortingScheduled() || !section.hasTranslucentGeometry()) return;
		section.resortTransparency(sectionRenderDispatcher);
		ResortGate.scheduled();
	}
}
