package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.PoiColumns;
import com.chopstickshq.quillcore.logic.PoiColumnsHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Every square/range POI search walks each chunk's sections one hash lookup at a time. With the column mask the empty
 * sections are skipped without a lookup and a column with no POI storage costs one lookup. Same sections, same order
 * (lowest first), so the stream is identical. Unknown columns take the vanilla path, which also loads them.
 */
@Mixin(PoiManager.class)
public abstract class PoiManagerColumnsMixin {
	@Inject(method = "getInChunk", at = @At("HEAD"), cancellable = true)
	private void quillcore$maskedChunk(Predicate<Holder<PoiType>> type, ChunkPos chunk, PoiManager.Occupancy occupancy,
			CallbackInfoReturnable<Stream<PoiRecord>> cir) {
		PoiColumnsHolder self = (PoiColumnsHolder) this;
		PoiColumns cols = self.quillcore$columns();
		if (cols == null) return;
		long mask = cols.mask(chunk.toLong());
		if (mask == PoiColumns.UNKNOWN) return;
		if (mask == 0) {
			cir.setReturnValue(Stream.empty());
			return;
		}
		int min = self.quillcore$minSection();
		if (Long.bitCount(mask) == 1) {
			cir.setReturnValue(quillcore$section(self, chunk, min + Long.numberOfTrailingZeros(mask)).getRecords(type, occupancy));
			return;
		}
		List<PoiSection> sections = new ArrayList<>(Long.bitCount(mask));
		for (long m = mask; m != 0; m &= m - 1) {
			sections.add(quillcore$section(self, chunk, min + Long.numberOfTrailingZeros(m)));
		}
		cir.setReturnValue(sections.stream().flatMap(s -> s.getRecords(type, occupancy)));
	}

	@Unique
	private static PoiSection quillcore$section(PoiColumnsHolder self, ChunkPos chunk, int y) {
		Optional<?> o = self.quillcore$stored(SectionPos.asLong(chunk.x, y, chunk.z));
		return (PoiSection) o.get();
	}
}
