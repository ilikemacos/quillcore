package com.chopstickshq.quillcore.logic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Comparator;
import java.util.List;

import com.chopstickshq.quillcore.logic.DistanceSort;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The sensor sorts nearby mobs by distance with a comparator that works the distance out twice per comparison. Work
 * each distance out once into primitive keys, then run a stable sort on them (DistanceSort), so the order is identical (ties keep their
 * original order, Double.compare semantics). Kill switch: -Dquillcore.vanillaSensorSort=true.
 */
@Mixin(NearestLivingEntitySensor.class)
public abstract class NearestLivingSensorSortMixin {
	@Unique
	private static final boolean QUILLCORE$VANILLA = Boolean.getBoolean("quillcore.vanillaSensorSort");

	@WrapOperation(method = "doTick", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"))
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void quillcore$sort(List list, Comparator cmp, Operation<Void> original, @Local(argsOnly = true) LivingEntity self) {
		int n = list.size();
		if (QUILLCORE$VANILLA || n < 2) {
			original.call(list, cmp);
			return;
		}
		long[] k = new long[n];
		for (int i = 0; i < n; i++) k[i] = DistanceSort.sortable(self.distanceToSqr((LivingEntity) list.get(i)));
		DistanceSort.sort(list, k);
	}
}
