package com.chopstickshq.quillcore.memory.mixin;

import com.chopstickshq.quillcore.memory.ShapeInterner;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseShapesMixin {
	@Shadow private VoxelShape occlusionShape;
	@Shadow private VoxelShape[] occlusionShapesByFace;
	@Shadow @Final private static VoxelShape[] EMPTY_OCCLUSION_SHAPES;
	@Shadow @Final private static VoxelShape[] FULL_BLOCK_OCCLUSION_SHAPES;

	@Inject(method = "initCache", at = @At("RETURN"))
	private void quillcore$share(CallbackInfo ci) {
		occlusionShape = ShapeInterner.intern(occlusionShape);
		VoxelShape[] faces = occlusionShapesByFace;
		// the two shared constants are already one instance each; leave them alone
		if (faces == null || faces == EMPTY_OCCLUSION_SHAPES || faces == FULL_BLOCK_OCCLUSION_SHAPES) return;
		for (int i = 0; i < faces.length; i++) faces[i] = ShapeInterner.intern(faces[i]);
	}
}
