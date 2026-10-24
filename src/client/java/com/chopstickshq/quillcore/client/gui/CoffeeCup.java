package com.chopstickshq.quillcore.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/** A tiny cup: body, handle, and a wisp of steam. Nine pixels square. */
final class CoffeeCup {
	static final int SIZE = 9;

	private CoffeeCup() {}

	static void draw(GuiGraphics g, int x, int y, int color) {
		g.renderOutline(x, y + 3, 7, 6, color);
		g.fill(x + 1, y + 6, x + 6, y + 8, color);
		g.renderOutline(x + 7, y + 4, 2, 3, color);
		g.vLine(x + 2, y - 1, y + 1, color);
		g.vLine(x + 4, y - 2, y, color);
	}
}
