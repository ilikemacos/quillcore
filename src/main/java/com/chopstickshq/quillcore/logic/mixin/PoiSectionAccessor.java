package com.chopstickshq.quillcore.logic.mixin;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PoiSection.class)
public interface PoiSectionAccessor {
	@Accessor("byType")
	Map<Holder<PoiType>, Set<PoiRecord>> quillcore$byType();
}
