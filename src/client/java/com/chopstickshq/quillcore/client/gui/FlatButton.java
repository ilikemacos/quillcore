package com.chopstickshq.quillcore.client.gui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Outlined text button that fills with ink on hover. */
final class FlatButton {
	private final int x, y, w, h;
	private final String label;
	private final boolean coffee;
	private final BooleanSupplier enabled;
	private final Runnable action;

	private FlatButton(int x, int y, int w, int h, String label, boolean coffee, BooleanSupplier enabled, Runnable action) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.label = label;
		this.coffee = coffee;
		this.enabled = enabled;
		this.action = action;
	}

	static FlatButton text(int x, int y, int w, String label, BooleanSupplier enabled, Runnable action) {
		return new FlatButton(x, y, w, QuillLayout.BUTTON_H, label, false, enabled, action);
	}

	static FlatButton coffee(Font font, int x, int y, String label, Runnable action) {
		return new FlatButton(x, y, coffeeWidth(font, label), QuillLayout.BUTTON_H, label, true, () -> true, action);
	}

	static int coffeeWidth(Font font, String label) {
		return font.width(label) + CoffeeCup.SIZE + 16;
	}

	boolean contains(double mx, double my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	void render(GuiGraphics g, Font font, HoverFade fade, int mx, int my, boolean interactive) {
		boolean on = enabled.getAsBoolean();
		float t = fade.update(this, interactive && on && contains(mx, my));
		int border = on ? QuillPalette.INK : QuillPalette.INK_FAINT;
		int idleText = on ? (coffee ? QuillPalette.OXBLOOD : QuillPalette.INK) : QuillPalette.INK_FAINT;
		int text = QuillPalette.mix(idleText, QuillPalette.PAPER, t);
		g.fill(x, y, x + w, y + h, QuillPalette.mix(QuillPalette.PAPER, QuillPalette.INK, t));
		g.renderOutline(x, y, w, h, coffee && on ? QuillPalette.mix(QuillPalette.OXBLOOD, QuillPalette.INK, t) : border);
		int ty = y + (h - 8) / 2;
		if (coffee) {
			CoffeeCup.draw(g, x + 6, ty - 1, text);
			g.drawString(font, label, x + 10 + CoffeeCup.SIZE, ty, text, false);
		} else {
			g.drawString(font, label, x + (w - font.width(label)) / 2, ty, text, false);
		}
	}

	boolean click(double mx, double my) {
		if (enabled.getAsBoolean() && contains(mx, my)) {
			action.run();
			return true;
		}
		return false;
	}
}
