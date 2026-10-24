package com.chopstickshq.quillcore.client;

import com.chopstickshq.quillcore.client.gui.QuillVideoSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Soft ModMenu integration. Opens the QuillCore video settings screen (no extra deps);
 * the full Cloth Config screen is reachable from its Advanced tab when Cloth Config is installed.
 */
public final class QuillCoreModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return QuillVideoSettingsScreen::new;
	}
}
