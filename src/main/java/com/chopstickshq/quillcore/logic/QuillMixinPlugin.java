package com.chopstickshq.quillcore.logic;

import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Turns off the logic mixins that hook the same calls as Lithium when Lithium is installed. Two exclusive hooks on one
 * call crash the game on launch; Lithium already speeds those paths up, so stepping aside loses nothing.
 */
public final class QuillMixinPlugin implements IMixinConfigPlugin {
	private static final String PKG = "com.chopstickshq.quillcore.logic.mixin.";
	private static final Set<String> OVERLAPS_LITHIUM = Set.of(
			"EntityCollideSkipMixin",
			"EntitySectionCollidersMixin",
			"BlockCollisionsFastMixin",
			"PistonBaseBlockDirectionsMixin",
			"PistonStructureResolverDirectionsMixin",
			"RedStoneWireBlockDirectionsMixin",
			"PalettedContainerNoLockMixin",
			"BrainFlatBehaviorsMixin");

	private boolean lithium;

	@Override
	public void onLoad(String mixinPackage) {
		lithium = FabricLoader.getInstance().isModLoaded("lithium");
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
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
