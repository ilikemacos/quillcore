package com.chopstickshq.quillcore.client.gui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** The occasional coffee note. Closes on any of its buttons, Escape, or a click outside it. */
final class SupportCard {
	private static final int MAX_W = 280;
	private static final int H = 84;
	private static final String MESSAGE =
			"QuillCore is made in spare hours. If it bought you some frames, a coffee helps keep it going.";

	private final Font font;
	private final Runnable onCoffee;
	private final Runnable onNeverAgain;
	private boolean open;
	private int areaX, areaY, areaR, areaB;
	private int x, y, w;
	private List<FlatButton> buttons = List.of();

	SupportCard(Font font, boolean open, Runnable onCoffee, Runnable onNeverAgain) {
		this.font = font;
		this.open = open;
		this.onCoffee = onCoffee;
		this.onNeverAgain = onNeverAgain;
	}

	boolean isOpen() {
		return open;
	}

	void dismiss() {
		open = false;
	}

	void place(int areaX, int areaY, int areaR, int areaB) {
		this.areaX = areaX;
		this.areaY = areaY;
		this.areaR = areaR;
		this.areaB = areaB;
		w = Math.min(MAX_W, areaR - areaX - 2 * QuillLayout.PAD);
		x = (areaX + areaR - w) / 2;
		y = Math.max(areaY + QuillLayout.PAD, (areaY + areaB - H) / 2);
		int by = y + H - QuillLayout.BUTTON_H - 8;
		int never = buttonWidth("Don't show again");
		int later = buttonWidth("Not now");
		int coffee = w - 2 * 8 - never - later - 2 * 4;
		buttons = List.of(
				FlatButton.text(x + 8, by, coffee, "Buy a coffee", () -> true, () -> {
					open = false;
					onCoffee.run();
				}),
				FlatButton.text(x + 8 + coffee + 4, by, later, "Not now", () -> true, this::dismiss),
				FlatButton.text(x + w - 8 - never, by, never, "Don't show again", () -> true, () -> {
					open = false;
					onNeverAgain.run();
				}));
	}

	private int buttonWidth(String label) {
		return font.width(label) + 12;
	}

	/** Consumes every click while open; a click outside the card just closes it. */
	boolean click(double mx, double my) {
		if (!open) {
			return false;
		}
		for (FlatButton b : buttons) {
			if (b.click(mx, my)) {
				return true;
			}
		}
		if (!(mx >= x && mx < x + w && my >= y && my < y + H)) {
			dismiss();
		}
		return true;
	}

	void render(GuiGraphics g, HoverFade fade, int mx, int my) {
		if (!open) {
			return;
		}
		g.fill(areaX, areaY, areaR, areaB, QuillPalette.VEIL);
		g.fill(x + 2, y + 2, x + w + 2, y + H + 2, QuillPalette.DROP_SHADOW);
		g.fill(x, y, x + w, y + H, QuillPalette.PAPER);
		g.renderOutline(x, y, w, H, QuillPalette.INK);
		g.renderOutline(x + 2, y + 2, w - 4, H - 4, QuillPalette.RULE);
		CoffeeCup.draw(g, x + 8, y + 8, QuillPalette.OXBLOOD);
		SmallCaps.draw(g, font, "A small note", x + 12 + CoffeeCup.SIZE, y + 9, QuillPalette.OXBLOOD);
		List<FormattedCharSequence> lines = font.split(Component.literal(MESSAGE), w - 16);
		for (int i = 0; i < Math.min(3, lines.size()); i++) {
			g.drawString(font, lines.get(i), x + 8, y + 24 + i * 10, QuillPalette.INK_SOFT, false);
		}
		for (FlatButton b : buttons) {
			b.render(g, font, fade, mx, my, true);
		}
	}
}
