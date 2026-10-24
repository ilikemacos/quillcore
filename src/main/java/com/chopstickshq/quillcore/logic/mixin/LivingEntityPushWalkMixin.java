package com.chopstickshq.quillcore.logic.mixin;

import com.chopstickshq.quillcore.logic.PushWalk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Walks the pushable neighbours in place instead of collecting them (see {@link PushWalk}). Falls back to vanilla on
 * the client, when ender dragon parts exist, and whenever the cramming check could fire, so the random roll and the
 * damage happen exactly as vanilla does them.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPushWalkMixin implements PushWalk.PushTarget {
	@Unique
	private PushWalk quillcore$walk;

	@Shadow
	protected abstract void doPush(Entity entity);

	@Override
	public void quillcore$doPush(Entity other) {
		doPush(other);
	}

	@Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
	private void quillcore$walkPush(CallbackInfo ci) {
		if (PushWalk.forceVanilla) return;
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self.level() instanceof ServerLevel level) || !level.dragonParts().isEmpty()) return;
		if (!(((LevelEntitiesAccessor) level).quillcore$entities() instanceof LevelEntityGetterAdapter<?> adapter)) return;
		@SuppressWarnings("unchecked")
		EntitySectionStorage<Entity> storage = (EntitySectionStorage<Entity>) ((LevelEntityGetterAdapterAccessor) adapter).quillcore$sections();

		Team team = self.getTeam();
		Team.CollisionRule rule = team == null ? Team.CollisionRule.ALWAYS : team.getCollisionRule();
		if (rule == Team.CollisionRule.NEVER) {
			ci.cancel();
			return;
		}
		PushWalk walk = quillcore$walk;
		if (walk == null) walk = quillcore$walk = new PushWalk();
		walk.begin(self, team, rule, false);
		storage.getEntities(self.getBoundingBox(), walk);
		int n = walk.count();
		if (n == 0) {
			walk.end();
			ci.cancel();
			return;
		}
		int cram = level.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
		if (cram > 0 && n > cram - 1) {
			walk.end();
			return;
		}
		walk.begin(self, team, rule, true);
		storage.getEntities(self.getBoundingBox(), walk);
		walk.end();
		ci.cancel();
	}
}
