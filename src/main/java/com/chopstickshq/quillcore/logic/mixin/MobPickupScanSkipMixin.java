package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.ItemCount;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.List;

/** Skips the loot pickup scan when no nearby entity section holds an item (see {@link ItemCount}). */
@Mixin(Mob.class)
public abstract class MobPickupScanSkipMixin {
	@WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
	private List<?> quillcore$skipEmpty(Level level, Class<?> cls, AABB box, Operation<List<?>> original) {
		if (!ItemCount.FORCE_VANILLA && level instanceof ServerLevel
				&& ((LevelEntitiesAccessor) level).quillcore$entities() instanceof LevelEntityGetterAdapter<?> adapter) {
			@SuppressWarnings("unchecked")
			EntitySectionStorage<Entity> storage = (EntitySectionStorage<Entity>) ((LevelEntityGetterAdapterAccessor) adapter).quillcore$sections();
			if (ItemCount.noItemsNear(storage, box)) return Collections.emptyList();
		}
		return original.call(level, cls, box);
	}
}
