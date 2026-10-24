package com.chopstickshq.quillcore.logic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The sensor sorts nearby mobs by distance with a comparator that works the distance out twice per comparison. Work
 * each distance out once, then run the same stable sort on the same keys, so the order is identical (ties keep their
 * original order, Double.compare semantics). Kill switch: -Dquillcore.vanillaSensorSort=true.
 */
@Mixin(NearestLivingEntitySensor.class)
public abstract class NearestLivingSensorSortMixin {
	@Unique
	private static final boolean QUILLCORE$VANILLA = Boolean.getBoolean("quillcore.vanillaSensorSort");

	@Unique
	private record Keyed(double d, LivingEntity e) {}

	@Unique
	private static final Comparator<Keyed> QUILLCORE$BY_D = (a, b) -> Double.compare(a.d, b.d);

	@WrapOperation(method = "doTick", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"))
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void quillcore$sort(List list, Comparator cmp, Operation<Void> original, @Local(argsOnly = true) LivingEntity self) {
		int n = list.size();
		if (QUILLCORE$VANILLA || n < 2) {
			original.call(list, cmp);
			return;
		}
		Keyed[] k = new Keyed[n];
		for (int i = 0; i < n; i++) {
			LivingEntity e = (LivingEntity) list.get(i);
			k[i] = new Keyed(self.distanceToSqr(e), e);
		}
		Arrays.sort(k, QUILLCORE$BY_D);
		for (int i = 0; i < n; i++) list.set(i, k[i].e);
	}
}
