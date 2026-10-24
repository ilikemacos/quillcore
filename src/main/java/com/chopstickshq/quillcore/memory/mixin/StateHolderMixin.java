package com.chopstickshq.quillcore.memory.mixin;

import com.chopstickshq.quillcore.memory.StateTable;
import com.chopstickshq.quillcore.memory.StateTables;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/** Replaces the per-state neighbour map with one shared table per block. */
@Mixin(StateHolder.class)
public abstract class StateHolderMixin<O, S> {
	@Shadow @Final private Reference2ObjectArrayMap<Property<?>, Comparable<?>> values;

	@Unique private StateTable<Property<?>, S> quillcore$table;
	@Unique private int quillcore$index;

	@Inject(method = "populateNeighbours", at = @At("HEAD"), cancellable = true)
	private void quillcore$buildTable(Map<Map<Property<?>, Comparable<?>>, S> all, CallbackInfo ci) {
		if (quillcore$table != null) {
			throw new IllegalStateException();
		}
		quillcore$table = StateTables.forDefinition(all, values);
		quillcore$index = StateTables.indexOf(quillcore$table, values);
		ci.cancel();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "setValueInternal", at = @At("HEAD"), cancellable = true)
	private void quillcore$lookup(Property property, Comparable value, Comparable current, CallbackInfoReturnable<Object> cir) {
		StateTable<Property<?>, S> table = quillcore$table;
		if (table == null || value.equals(current)) {
			return;
		}
		int to = property.getInternalIndex(value);
		if (to < 0) {
			return; // let vanilla throw its usual message
		}
		cir.setReturnValue(table.neighbour(quillcore$index, property, property.getInternalIndex(current), to));
	}
}
