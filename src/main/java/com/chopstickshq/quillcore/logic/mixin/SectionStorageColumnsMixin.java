package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.PoiColumns;
import com.chopstickshq.quillcore.logic.PoiColumnsHolder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Keeps a per-column bitmask of the sections that have storage. Storage entries are only ever added: a whole column
 * when it is unpacked , or one section in getOrCreate. Nothing removes them, so those two places are enough.
 */
@Mixin(SectionStorage.class)
public abstract class SectionStorageColumnsMixin<R> implements PoiColumnsHolder {
	@Shadow @Final private Long2ObjectMap<Optional<R>> storage;
	@Shadow @Final protected LevelHeightAccessor levelHeightAccessor;

	@Unique
	private PoiColumns quillcore$columns;

	@Override
	public PoiColumns quillcore$columns() {
		if (quillcore$columns == null && levelHeightAccessor.getSectionsCount() <= PoiColumns.MAX_SECTIONS) {
			quillcore$columns = new PoiColumns();
		}
		return quillcore$columns;
	}

	@Override
	public int quillcore$minSection() {
		return levelHeightAccessor.getMinSectionY();
	}

	@Override
	public Optional<?> quillcore$stored(long section) {
		return storage.get(section);
	}

	@Inject(method = "unpackChunk(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/storage/SectionStorage$PackedChunk;)V",
			at = @At("TAIL"))
	private void quillcore$columnLoaded(ChunkPos chunk, @Coerce Object packed, CallbackInfo ci) {
		PoiColumns cols = quillcore$columns();
		if (cols == null) return;
		int min = levelHeightAccessor.getMinSectionY();
		int n = levelHeightAccessor.getSectionsCount();
		long mask = 0;
		for (int i = 0; i < n; i++) {
			Optional<R> o = storage.get(SectionPos.asLong(chunk.x, min + i, chunk.z));
			if (o != null && o.isPresent()) mask |= 1L << i;
		}
		cols.column(chunk.toLong(), mask);
	}

	@Inject(method = "getOrCreate", at = @At("RETURN"))
	private void quillcore$sectionCreated(long section, CallbackInfoReturnable<R> cir) {
		PoiColumns cols = quillcore$columns();
		if (cols == null) return;
		cols.section(ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section)),
				SectionPos.y(section) - levelHeightAccessor.getMinSectionY());
	}
}
