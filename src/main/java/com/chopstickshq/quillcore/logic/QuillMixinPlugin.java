package com.chopstickshq.quillcore.logic;

import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Turns off our mixins that hook the same code as Lithium or FerriteCore when those are installed. Two exclusive hooks
 * on one call crash the game, and the other mod already speeds that path up, so stepping aside loses nothing.
 * Used by both quillcore.mixins.json and quillcore.memory.mixins.json.
 */
public final class QuillMixinPlugin implements IMixinConfigPlugin {
	private static final String PKG = "com.chopstickshq.quillcore.logic.mixin.";
	private static final String MEMORY_PKG = "com.chopstickshq.quillcore.memory.mixin.";
	private static final Set<String> OVERLAPS_LITHIUM = Set.of(
			"PoiManagerRangeLoopsMixin",
			"EntityCollideSkipMixin",
			"EntitySectionCollidersMixin",
			"LivingEntityPushWalkMixin",
			"MobPickupScanSkipMixin",
			"BlockCollisionsFastMixin",
			"PistonBaseBlockDirectionsMixin",
			"PistonStructureResolverDirectionsMixin",
			"RedStoneWireBlockDirectionsMixin",
			"PalettedContainerNoLockMixin",
			"BrainFlatBehaviorsMixin");

	private boolean lithium;
	private boolean ferritecore;

	@Override
	public void onLoad(String mixinPackage) {
		lithium = FabricLoader.getInstance().isModLoaded("lithium");
		ferritecore = FabricLoader.getInstance().isModLoaded("ferritecore");
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		// Our whole memory package (shared state tables, shape caches) does what FerriteCore does.
		if (mixinClassName.startsWith(MEMORY_PKG)) return !ferritecore;
		if (!lithium || !mixinClassName.startsWith(PKG)) return true;
		return !OVERLAPS_LITHIUM.contains(mixinClassName.substring(PKG.length()));
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
