package com.chopstickshq.quillcore.logic.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

/**
 * GateBehavior runs its children through streams every tick, building a fresh pipeline each time. The streams are
 * sequential and lazy, so each one visits the children in list order and tests a child's status just before acting
 * on it. These loops do the same, in the same order, with the same short-circuits (findFirst for RUN_ONE,
 * noneMatch for the stop check). Kill switch: -Dquillcore.vanillaGate=true.
 */
@Mixin(GateBehavior.class)
public abstract class GateBehaviorLoopsMixin<E extends LivingEntity> {
	@Unique
	private static final boolean QUILLCORE$VANILLA = Boolean.getBoolean("quillcore.vanillaGate");

	@Shadow @Final private ShufflingList<BehaviorControl<? super E>> behaviors;
	@Shadow @Final private GateBehavior.OrderPolicy orderPolicy;
	@Shadow @Final private GateBehavior.RunningPolicy runningPolicy;
	@Shadow @Final private Set<MemoryModuleType<?>> exitErasedMemories;
	@Shadow private Behavior.Status status;

	@Shadow
	protected abstract boolean hasRequiredMemories(E entity);

	@Unique
	private static boolean quillcore$off() {
		return QUILLCORE$VANILLA || com.chopstickshq.quillcore.logic.GateLoops.forceVanilla;
	}

	@Unique
	@SuppressWarnings("unchecked")
	private List<ShufflingList.WeightedEntry<BehaviorControl<? super E>>> quillcore$entries() {
		return ((ShufflingListAccessor<BehaviorControl<? super E>>) (Object) behaviors).quillcore$entries();
	}

	@Inject(method = "tryStart", at = @At("HEAD"), cancellable = true)
	private void quillcore$tryStart(ServerLevel level, E entity, long time, CallbackInfoReturnable<Boolean> cir) {
		if (quillcore$off()) return;
		GateBehavior.RunningPolicy policy = runningPolicy;
		boolean one = policy == GateBehavior.RunningPolicy.RUN_ONE;
		if (!one && policy != GateBehavior.RunningPolicy.TRY_ALL) return;
		if (!hasRequiredMemories(entity)) {
			cir.setReturnValue(false);
			return;
		}
		status = Behavior.Status.RUNNING;
		orderPolicy.apply(behaviors);
		List<ShufflingList.WeightedEntry<BehaviorControl<? super E>>> list = quillcore$entries();
		for (int i = 0, n = list.size(); i < n; i++) {
			BehaviorControl<? super E> b = list.get(i).getData();
			if (b.getStatus() != Behavior.Status.STOPPED) continue;
			if (b.tryStart(level, entity, time) && one) break;
		}
		cir.setReturnValue(true);
	}

	@Inject(method = "tickOrStop", at = @At("HEAD"), cancellable = true)
	private void quillcore$tickOrStop(ServerLevel level, E entity, long time, CallbackInfo ci) {
		if (quillcore$off()) return;
		List<ShufflingList.WeightedEntry<BehaviorControl<? super E>>> list = quillcore$entries();
		int n = list.size();
		for (int i = 0; i < n; i++) {
			BehaviorControl<? super E> b = list.get(i).getData();
			if (b.getStatus() == Behavior.Status.RUNNING) b.tickOrStop(level, entity, time);
		}
		boolean any = false;
		for (int i = 0; i < n; i++) {
			if (list.get(i).getData().getStatus() == Behavior.Status.RUNNING) {
				any = true;
				break;
			}
		}
		if (!any) ((GateBehavior<E>) (Object) this).doStop(level, entity, time);
		ci.cancel();
	}

	@Inject(method = "doStop", at = @At("HEAD"), cancellable = true)
	private void quillcore$doStop(ServerLevel level, E entity, long time, CallbackInfo ci) {
		if (quillcore$off()) return;
		status = Behavior.Status.STOPPED;
		List<ShufflingList.WeightedEntry<BehaviorControl<? super E>>> list = quillcore$entries();
		for (int i = 0, n = list.size(); i < n; i++) {
			BehaviorControl<? super E> b = list.get(i).getData();
			if (b.getStatus() == Behavior.Status.RUNNING) b.doStop(level, entity, time);
		}
		for (MemoryModuleType<?> m : exitErasedMemories) entity.getBrain().eraseMemory(m);
		ci.cancel();
	}
}
