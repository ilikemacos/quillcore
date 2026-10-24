package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.PoiRange;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Whole-range POI queries (closest, count, closest-first list) without the stream pipeline. The records and the order
 * they are seen in are the same as vanilla's (see PoiRange). min keeps the first of equal distances, as Stream.min
 * does. The closest-first stream stays lazy until its terminal operation, then sorts stably, like Stream.sorted.
 * Only short-circuiting queries (find, take) keep vanilla's streams, since those must stop loading chunks early.
 */
@Mixin(PoiManager.class)
public abstract class PoiManagerRangeLoopsMixin {
	private List<PoiRecord> quillcore$range(Predicate<Holder<PoiType>> type, BlockPos pos, int r, PoiManager.Occupancy occ) {
		List<PoiRecord> out = new ArrayList<>();
		PoiRange.collect((PoiManager) (Object) this, type, pos, r, occ, out);
		return out;
	}

	private static PoiRecord quillcore$closest(List<PoiRecord> l, BlockPos pos, Predicate<BlockPos> extra) {
		PoiRecord best = null;
		double bd = 0;
		for (int i = 0; i < l.size(); i++) {
			PoiRecord rec = l.get(i);
			if (extra != null && !extra.test(rec.getPos())) continue;
			double d = rec.getPos().distSqr(pos);
			if (best == null || d < bd) {
				best = rec;
				bd = d;
			}
		}
		return best;
	}

	@Inject(method = "getCountInRange", at = @At("HEAD"), cancellable = true)
	private void quillcore$count(Predicate<Holder<PoiType>> type, BlockPos pos, int r, PoiManager.Occupancy occ,
			CallbackInfoReturnable<Long> cir) {
		if (PoiRange.vanilla()) return;
		cir.setReturnValue((long) quillcore$range(type, pos, r, occ).size());
	}

	@Inject(method = "findClosest(Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;",
			at = @At("HEAD"), cancellable = true)
	private void quillcore$closest(Predicate<Holder<PoiType>> type, BlockPos pos, int r, PoiManager.Occupancy occ,
			CallbackInfoReturnable<Optional<BlockPos>> cir) {
		if (PoiRange.vanilla()) return;
		PoiRecord b = quillcore$closest(quillcore$range(type, pos, r, occ), pos, null);
		cir.setReturnValue(b == null ? Optional.empty() : Optional.of(b.getPos()));
	}

	@Inject(method = "findClosest(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;",
			at = @At("HEAD"), cancellable = true)
	private void quillcore$closestFiltered(Predicate<Holder<PoiType>> type, Predicate<BlockPos> extra, BlockPos pos, int r,
			PoiManager.Occupancy occ, CallbackInfoReturnable<Optional<BlockPos>> cir) {
		if (PoiRange.vanilla()) return;
		PoiRecord b = quillcore$closest(quillcore$range(type, pos, r, occ), pos, extra);
		cir.setReturnValue(b == null ? Optional.empty() : Optional.of(b.getPos()));
	}

	@Inject(method = "findClosestWithType", at = @At("HEAD"), cancellable = true)
	private void quillcore$closestWithType(Predicate<Holder<PoiType>> type, BlockPos pos, int r, PoiManager.Occupancy occ,
			CallbackInfoReturnable<Optional<Pair<Holder<PoiType>, BlockPos>>> cir) {
		if (PoiRange.vanilla()) return;
		PoiRecord b = quillcore$closest(quillcore$range(type, pos, r, occ), pos, null);
		cir.setReturnValue(b == null ? Optional.empty() : Optional.of(Pair.of(b.getPoiType(), b.getPos())));
	}

	@Inject(method = "findAllClosestFirstWithType", at = @At("HEAD"), cancellable = true)
	private void quillcore$closestFirst(Predicate<Holder<PoiType>> type, Predicate<BlockPos> extra, BlockPos pos, int r,
			PoiManager.Occupancy occ, CallbackInfoReturnable<Stream<Pair<Holder<PoiType>, BlockPos>>> cir) {
		if (PoiRange.vanilla()) return;
		cir.setReturnValue(StreamSupport.stream(() -> {
			List<PoiRecord> l = quillcore$range(type, pos, r, occ);
			List<Pair<Holder<PoiType>, BlockPos>> out = new ArrayList<>(l.size());
			for (int i = 0; i < l.size(); i++) {
				PoiRecord rec = l.get(i);
				if (extra.test(rec.getPos())) out.add(Pair.of(rec.getPoiType(), rec.getPos()));
			}
			out.sort(Comparator.comparingDouble(p -> p.getSecond().distSqr(pos)));
			return out.spliterator();
		}, Spliterator.ORDERED, false));
	}
}
