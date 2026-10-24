package com.chopstickshq.quillcore.logic;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;

/**
 * Which entity classes can ever block another entity's movement. {@code Entity.canBeCollidedWith} returns false unless
 * a subclass overrides it (boats, shulkers, happy ghasts, and whatever mods add), so a class that inherits it unchanged
 * never collides. If the method name can't be resolved, every class counts as a collider and nothing is skipped.
 */
public final class HardColliders {
	private static final String NAME = resolve();

	private static final ClassValue<Boolean> OVERRIDES = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> c) {
			if (NAME == null) return true;
			for (Class<?> k = c; k != null && k != Entity.class; k = k.getSuperclass()) {
				try {
					k.getDeclaredMethod(NAME, Entity.class);
					return true;
				} catch (NoSuchMethodException ignored) {
				} catch (Throwable t) {
					return true;
				}
			}
			return false;
		}
	};

	private HardColliders() {
	}

	private static String resolve() {
		try {
			String n = FabricLoader.getInstance().getMappingResolver()
					.mapMethodName("intermediary", "net.minecraft.class_1297", "method_30948", "(Lnet/minecraft/class_1297;)Z");
			Entity.class.getDeclaredMethod(n, Entity.class);
			return n;
		} catch (Throwable t) {
			return null;
		}
	}

	public static boolean mayCollide(Object entity) {
		return !(entity instanceof Entity) || OVERRIDES.get(entity.getClass());
	}
}
