package com.chopstickshq.quillcore.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Uppercase with shrunken lowercase letters and 1px tracking, read as small caps. */
final class SmallCaps {
	private static final float LOWER_SCALE = 0.78f;

	private SmallCaps() {}

	static void draw(GuiGraphics g, Font font, String s, int x, int y, int color) {
		float cx = x;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			String up = String.valueOf(Character.toUpperCase(ch));
			float scale = Character.isLowerCase(ch) ? LOWER_SCALE : 1f;
			g.pose().pushMatrix();
			g.pose().translate(cx, y + (1f - scale) * 7f);
			g.pose().scale(scale, scale);
			g.drawString(font, up, 0, 0, color, false);
			g.pose().popMatrix();
			cx += font.width(up) * scale + 1;
		}
	}

	static int width(Font font, String s) {
		float w = 0;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			int cw = font.width(String.valueOf(Character.toUpperCase(ch)));
			w += (Character.isLowerCase(ch) ? cw * LOWER_SCALE : cw) + 1;
		}
		return Math.round(w);
	}
}
