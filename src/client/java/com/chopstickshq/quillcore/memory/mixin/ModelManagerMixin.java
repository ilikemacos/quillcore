package com.chopstickshq.quillcore.memory.mixin;

import com.chopstickshq.quillcore.memory.QuadData;
import net.minecraft.client.resources.model.ModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drop the quad pool once a reload is applied so old models can be collected. */
@Mixin(ModelManager.class)
public abstract class ModelManagerMixin {
	@Unique private static final Logger LOGGER = LoggerFactory.getLogger("quillcore");

	@Inject(method = "apply", at = @At("RETURN"))
	private void quillcore$dropQuadPool(CallbackInfo ci) {
		LOGGER.info("quad vertex arrays: {} baked, {} unique", QuadData.VERTICES.seen(), QuadData.VERTICES.size());
		QuadData.VERTICES.clear();
	}
}
