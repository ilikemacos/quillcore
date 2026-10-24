package com.chopstickshq.quillcore.logic;

import com.chopstickshq.quillcore.logic.mixin.PoiSectionAccessor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;

/**
 * Loop version of PoiManager.getInRange for terminal operations that consume the whole stream. The visiting order is
 * the same as vanilla's: chunks z-major then x (as in ChunkPos.rangeClosed), sections lowest first, byType entries
 * in map order, then the records in set order. Each record passes the same filters, in the same order.
 */
public final class PoiRange {
	/** Test hook: true sends every call back to vanilla's streams. */
	public static volatile boolean forceVanilla;
	private static final boolean VANILLA = Boolean.getBoolean("quillcore.vanillaPoi");

	private PoiRange() {}

	public static boolean vanilla() {
		return VANILLA || forceVanilla;
	}

	public static void collect(PoiManager mgr, Predicate<Holder<PoiType>> type, BlockPos pos, int radius,
			PoiManager.Occupancy occupancy, List<PoiRecord> out) {
		int r = Math.floorDiv(radius, 16) + 1;
		ChunkPos c = new ChunkPos(pos);
		int rangeSq = radius * radius;
		Predicate<? super PoiRecord> occ = occupancy.getTest();
		PoiColumnsHolder self = (PoiColumnsHolder) (Object) mgr;
		PoiColumns cols = self.quillcore$columns();
		int min = self.quillcore$minSection();
		int start = out.size();
		for (int z = c.z - r; z <= c.z + r; z++) {
			for (int x = c.x - r; x <= c.x + r; x++) {
				long mask = cols == null ? PoiColumns.UNKNOWN : cols.mask(ChunkPos.asLong(x, z));
				if (mask == 0) continue;
				int before = out.size();
				if (mask == PoiColumns.UNKNOWN) {
					// Vanilla path, which also loads the column.
					mgr.getInChunk(type, new ChunkPos(x, z), occupancy).forEachOrdered(out::add);
				} else {
					for (long m = mask; m != 0; m &= m - 1) {
						Optional<?> o = self.quillcore$stored(SectionPos.asLong(x, min + Long.numberOfTrailingZeros(m), z));
						Map<Holder<PoiType>, Set<PoiRecord>> byType = ((PoiSectionAccessor) o.get()).quillcore$byType();
						for (Map.Entry<Holder<PoiType>, Set<PoiRecord>> e : byType.entrySet()) {
							if (!type.test(e.getKey())) continue;
							for (PoiRecord rec : e.getValue()) {
								if (occ.test(rec)) out.add(rec);
							}
						}
					}
				}
				// Square then range filter, kept in place.
				int w = before;
				for (int i = before; i < out.size(); i++) {
					PoiRecord rec = out.get(i);
					BlockPos p = rec.getPos();
					if (Math.abs(p.getX() - pos.getX()) <= radius && Math.abs(p.getZ() - pos.getZ()) <= radius
							&& p.distSqr(pos) <= rangeSq) {
						out.set(w++, rec);
					}
				}
				out.subList(w, out.size()).clear();
			}
		}
	}
}
