package com.chopstickshq.quillcore.logic.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.datafixers.util.Pair;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Nitwits search for a job site with {@link PoiType#NONE}, which matches nothing, so the 48-block
 * POI walk always comes back empty. Return the empty stream without walking. The cooldown and its
 * random roll happen before this call and are left alone.
 */
@Mixin(AcquirePoi.class)
public abstract class AcquirePoiNoneMixin {
	@Redirect(method = "*", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;findAllClosestFirstWithType(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/stream/Stream;"))
	private static Stream<Pair<Holder<PoiType>, BlockPos>> quillcore$skipNone(PoiManager poi, Predicate<Holder<PoiType>> type,
			Predicate<BlockPos> pos, BlockPos origin, int radius, PoiManager.Occupancy occupancy) {
		if (type == PoiType.NONE) return Stream.empty();
		return poi.findAllClosestFirstWithType(type, pos, origin, radius, occupancy);
	}
}
