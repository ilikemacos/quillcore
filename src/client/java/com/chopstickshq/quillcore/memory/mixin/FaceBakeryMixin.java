package com.chopstickshq.quillcore.memory.mixin;

import com.chopstickshq.quillcore.memory.QuadData;
import net.minecraft.client.renderer.block.model.FaceBakery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Share vertex arrays between quads that bake to the same data. */
@Mixin(FaceBakery.class)
public abstract class FaceBakeryMixin {
	@ModifyArg(method = "bakeQuad", index = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/model/BakedQuad;<init>([IILnet/minecraft/core/Direction;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;ZI)V"))
	private static int[] quillcore$shareVertices(int[] vertices) {
		return QuadData.VERTICES.intern(vertices);
	}
}
