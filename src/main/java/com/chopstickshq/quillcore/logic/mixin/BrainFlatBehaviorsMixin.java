package com.chopstickshq.quillcore.logic.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chopstickshq.quillcore.logic.BrainLoops;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Brain walks a TreeMap of HashMaps of LinkedHashSets twice per tick per mob. This keeps the same walk flattened into
 * arrays, rebuilt whenever behaviours are added or cleared, so the visiting order is exactly vanilla's (priority, then
 * the activity map's order, then insertion order). The active-activity check still happens once per (priority,
 * activity) group, at the moment vanilla would make it, and statuses are read live.
 * Kill switch: -Dquillcore.vanillaBrainLoops=true.
 */
@Mixin(Brain.class)
public abstract class BrainFlatBehaviorsMixin<E extends LivingEntity> {
	@Shadow
	@Final
	private Map<Integer, Map<Activity, Set<BehaviorControl<? super E>>>> availableBehaviorsByPriority;

	@Shadow
	@Final
	private Set<Activity> activeActivities;

	/** All behaviours in vanilla walk order. */
	@Unique
	private BehaviorControl<? super E>[] quillcore$flat;
	/** Group i covers quillcore$flat[groupStart[i] .. groupStart[i+1]). */
	@Unique
	private int[] quillcore$groupStart;
	@Unique
	private Activity[] quillcore$groupActivity;

	@Unique
	@SuppressWarnings("unchecked")
	private void quillcore$build() {
		List<BehaviorControl<? super E>> flat = new ArrayList<>();
		List<Activity> acts = new ArrayList<>();
		List<Integer> starts = new ArrayList<>();
		for (Map<Activity, Set<BehaviorControl<? super E>>> byActivity : availableBehaviorsByPriority.values()) {
			for (Map.Entry<Activity, Set<BehaviorControl<? super E>>> e : byActivity.entrySet()) {
				starts.add(flat.size());
				acts.add(e.getKey());
				flat.addAll(e.getValue());
			}
		}
		int[] gs = new int[starts.size() + 1];
		for (int i = 0; i < starts.size(); i++) gs[i] = starts.get(i);
		gs[starts.size()] = flat.size();
		quillcore$groupStart = gs;
		quillcore$groupActivity = acts.toArray(new Activity[0]);
		quillcore$flat = flat.toArray(new BehaviorControl[0]);
	}

	@Inject(method = {"addActivityAndRemoveMemoriesWhenStopped", "removeAllBehaviors"}, at = @At("TAIL"))
	private void quillcore$invalidate(CallbackInfo ci) {
		quillcore$flat = null;
	}

	@Inject(method = "getRunningBehaviors", at = @At("HEAD"), cancellable = true)
	private void quillcore$running(CallbackInfoReturnable<List<BehaviorControl<? super E>>> cir) {
		if (BrainLoops.forceVanilla) return;
		if (quillcore$flat == null) quillcore$build();
		List<BehaviorControl<? super E>> out = new ObjectArrayList<>();
		for (BehaviorControl<? super E> b : quillcore$flat) {
			if (b.getStatus() == Behavior.Status.RUNNING) out.add(b);
		}
		cir.setReturnValue(out);
	}

	@Inject(method = "startEachNonRunningBehavior", at = @At("HEAD"), cancellable = true)
	private void quillcore$startEach(ServerLevel level, E entity, CallbackInfo ci) {
		if (BrainLoops.forceVanilla) return;
		if (quillcore$flat == null) quillcore$build();
		ci.cancel();
		long time = level.getGameTime();
		BehaviorControl<? super E>[] flat = quillcore$flat;
		int[] gs = quillcore$groupStart;
		Activity[] acts = quillcore$groupActivity;
		for (int g = 0; g < acts.length; g++) {
			if (!activeActivities.contains(acts[g])) continue;
			for (int i = gs[g], end = gs[g + 1]; i < end; i++) {
				BehaviorControl<? super E> b = flat[i];
				if (b.getStatus() == Behavior.Status.STOPPED) b.tryStart(level, entity, time);
			}
		}
	}
}
