package com.chopstickshq.quillcore.gametest;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Applies {@code -Dquillcore.test.logic=on|off} after QuillCore loaded its config.
 * "on" = every Logic throttle enabled with the most aggressive skip (10) so differences surface;
 * "off" = masterEnabled=false, i.e. vanilla behaviour. Both runs must pass the same GameTests.
 */
public final class QuillTestMode implements ModInitializer {
	public static final String PROP = "quillcore.test.logic";

	public static boolean logicOn() {
		return !"off".equalsIgnoreCase(System.getProperty(PROP, "on"));
	}

	public static void apply(boolean on) {
		QuillConfig c = QuillConfig.get();
		c.masterEnabled = on;
		c.logicEnabled = on;
		c.idleAiThrottleEnabled = on;
		c.brainIdleThrottleEnabled = on;
		c.itemMergeThrottleEnabled = on;
		c.pathNavThrottleEnabled = on;
		if (on) {
			c.idleAiThrottleMaxSkipTicks = 10;
			c.brainIdleThrottleMaxSkipTicks = 10;
			c.itemMergeThrottleMaxSkipTicks = 10;
			c.pathNavThrottleMaxSkipTicks = 10;
		}
	}

	@Override
	public void onInitialize() {
		if (System.getProperty(PROP) == null) return; // only GameTest runs set this; bench toggles itself
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			apply(logicOn());
			QuillCore.LOGGER.info("[quill-test] logic mode={} {}", logicOn() ? "on" : "off", QuillConfig.get().statusSummary());
		});
	}
}
