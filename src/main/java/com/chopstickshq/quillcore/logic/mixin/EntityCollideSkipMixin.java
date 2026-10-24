package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.HardColliderCount;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Every move asks for entity collision shapes, which walks every entity near the mover even though almost none can
 * block it. If no section the vanilla query would visit holds a possible collider, the answer is the empty list, so
 * return that without the walk. Otherwise call vanilla unchanged. Ender dragon parts live outside the sections but
 * never collide.
 */
@Mixin(Entity.class)
public abstract class EntityCollideSkipMixin {
	@Redirect(method = "collide", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
	private List<VoxelShape> quillcore$entityCollisions(Level level, Entity self, AABB box) {
		if (box.getSize() >= 1.0E-7 && !quillcore$anyCollider(level, box.inflate(1.0E-7))) return List.of();
		return level.getEntityCollisions(self, box);
	}

	@Unique
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static boolean quillcore$anyCollider(Level level, AABB box) {
		LevelEntityGetter<?> getter = ((LevelEntitiesAccessor) level).quillcore$entities();
		if (!(getter instanceof LevelEntityGetterAdapterAccessor a)) return true;
		EntitySectionStorage storage = a.quillcore$sections();
		boolean[] found = {false};
		storage.forEachAccessibleNonEmptySection(box, section -> {
			if (((HardColliderCount) section).quillcore$hardColliders() > 0) {
				found[0] = true;
				return AbortableIterationConsumer.Continuation.ABORT;
			}
			return AbortableIterationConsumer.Continuation.CONTINUE;
		});
		return found[0];
	}
}
