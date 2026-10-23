package com.chopstickshq.quillcore.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Soft ModMenu integration. Cloth Config must also be present to open the screen;
 * otherwise ModMenu gets a no-op factory that returns the parent.
 */
public final class QuillCoreModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
				return QuillCoreConfigScreen.create(parent);
			}
			return parent;
		};
	}
}
