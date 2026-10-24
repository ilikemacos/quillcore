package com.chopstickshq.quillcore.logic;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.Team;

/**
 * Server-side pushEntities without the list: the same section walk and the same filter as
 * Level.getPushableEntities, run twice. The first pass counts, the second pushes. Pushing only changes velocity,
 * so the set and the order seen by the second pass are the ones vanilla collected.
 * Set -Dquillcore.vanillaPush=true to use vanilla's pushEntities.
 */
public final class PushWalk implements AbortableIterationConsumer<Entity> {
	public static volatile boolean forceVanilla = Boolean.getBoolean("quillcore.vanillaPush");

	private LivingEntity self;
	private Team team;
	private Team.CollisionRule rule;
	private boolean push;
	private int count;

	public void begin(LivingEntity self, Team team, Team.CollisionRule rule, boolean push) {
		this.self = self;
		this.team = team;
		this.rule = rule;
		this.push = push;
		this.count = 0;
	}

	public int count() {
		return count;
	}

	public void end() {
		self = null;
		team = null;
	}

	@Override
	public Continuation accept(Entity other) {
		if (other != self && pushable(other)) {
			count++;
			if (push) ((PushTarget) self).quillcore$doPush(other);
		}
		return Continuation.CONTINUE;
	}

	/** EntitySelector.pushableBy on the server, with the pusher's team looked up once. */
	private boolean pushable(Entity other) {
		if (other.isSpectator() || !other.isPushable()) return false;
		Team otherTeam = other.getTeam();
		Team.CollisionRule otherRule = otherTeam == null ? Team.CollisionRule.ALWAYS : otherTeam.getCollisionRule();
		if (otherRule == Team.CollisionRule.NEVER) return false;
		boolean allied = team != null && team.isAlliedTo(otherTeam);
		if ((rule == Team.CollisionRule.PUSH_OWN_TEAM || otherRule == Team.CollisionRule.PUSH_OWN_TEAM) && allied) return false;
		return rule != Team.CollisionRule.PUSH_OTHER_TEAMS && otherRule != Team.CollisionRule.PUSH_OTHER_TEAMS || allied;
	}

	public interface PushTarget {
		void quillcore$doPush(Entity other);
	}
}
