package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.render.WeatherRadiusCap;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Optionally caps rain/snow column gather radius. Default off.
 */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherRadiusCapMixin {
	@ModifyArg(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;collectColumnInstances(Lnet/minecraft/world/level/Level;IFLnet/minecraft/world/phys/Vec3;ILjava/util/List;Ljava/util/List;)V"
			),
			index = 4
	)
	private int quillcore$capWeatherRadius(int radius) {
		QuillConfig cfg = QuillConfig.get();
		return WeatherRadiusCap.applyRadius(
				cfg.masterEnabled,
				cfg.renderEnabled,
				cfg.weatherRadiusCapEnabled,
				cfg.weatherRadiusCapBlocks,
				radius
		);
	}
}
