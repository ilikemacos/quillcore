package com.chopstickshq.quillcore.logic.mixin;

import net.minecraft.world.entity.ai.behavior.ShufflingList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ShufflingList.class)
public interface ShufflingListAccessor<U> {
	@Accessor("entries")
	List<ShufflingList.WeightedEntry<U>> quillcore$entries();
}
