package com.chopstickshq.quillcore.logic.mixin;

import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Skip the debug lock around palette writes. Vanilla only uses it to crash loudly when two
 * threads write one section at once; it costs a CAS on every block write.
 */
@Mixin(PalettedContainer.class)
public abstract class PalettedContainerNoLockMixin {
	/**
	 * @author ilikemacos
	 * @reason the lock only exists to detect misuse
	 */
	@Overwrite
	public void acquire() {
	}

	/**
	 * @author ilikemacos
	 * @reason see {@link #acquire()}
	 */
	@Overwrite
	public void release() {
	}
}
