package com.chopstickshq.quillcore.client.gui;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;

/** Eases a 0..1 value per key toward hovered or not, independent of frame rate. */
final class HoverFade {
	private static final float SECONDS_TO_FULL = 0.12f;

	private final Map<Object, Float> values = new HashMap<>();
	private long lastFrame = Util.getMillis();
	private float step;

	void tick() {
		long now = Util.getMillis();
		step = Math.min(1f, (now - lastFrame) / 1000f / SECONDS_TO_FULL);
		lastFrame = now;
	}

	float update(Object key, boolean hovered) {
		float current = values.getOrDefault(key, 0f);
		float next = hovered ? Math.min(1f, current + step) : Math.max(0f, current - step);
		values.put(key, next);
		return smooth(next);
	}

	private static float smooth(float t) {
		return t * t * (3f - 2f * t);
	}
}
