package com.chopstickshq.quillcore.render.mixin;

import com.chopstickshq.quillcore.render.prep.ChunkPrep;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Section draw prep without per-frame allocation, see {@link ChunkPrep}. Also places compact and slab draws. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererPrepMixin {
	@Shadow
	@Final
	private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

	@Inject(method = "prepareChunkRenders", at = @At("HEAD"), cancellable = true)
	private void quillcore$prepare(Matrix4fc modelView, double x, double y, double z, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
		cir.setReturnValue(ChunkPrep.prepare(visibleSections, modelView, x, y, z));
	}
}
