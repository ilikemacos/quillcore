package com.chopstickshq.quillcore.logic;

import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;

/**
 * Item entities per entity section, kept in step with EntitySection add and remove (a section move is a remove
 * plus an add, so it is covered too). Lets a pickup scan stop when no section it would visit holds an item.
 * Set -Dquillcore.vanillaItemScan=true to always run vanilla's scan.
 */
public interface ItemCount {
	boolean FORCE_VANILLA = Boolean.getBoolean("quillcore.vanillaItemScan");

	int quillcore$items();

	/** True when no section that EntitySectionStorage.getEntities would visit for {@code box} holds an item. */
	static boolean noItemsNear(EntitySectionStorage<Entity> storage, AABB box) {
		// Same section range as forEachAccessibleNonEmptySection.
		int x0 = SectionPos.posToSectionCoord(box.minX - 2.0), x1 = SectionPos.posToSectionCoord(box.maxX + 2.0);
		int y0 = SectionPos.posToSectionCoord(box.minY - 4.0), y1 = SectionPos.posToSectionCoord(box.maxY);
		int z0 = SectionPos.posToSectionCoord(box.minZ - 2.0), z1 = SectionPos.posToSectionCoord(box.maxZ + 2.0);
		if ((long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) > 64) return false;
		for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
			EntitySection<Entity> s = storage.getSection(SectionPos.asLong(x, y, z));
			if (s != null && ((ItemCount) s).quillcore$items() != 0) return false;
		}
		return true;
	}
}
