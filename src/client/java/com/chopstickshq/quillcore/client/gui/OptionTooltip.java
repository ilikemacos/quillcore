package com.chopstickshq.quillcore.client.gui;

import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Card that follows the cursor after it rests on an option for a moment. */
final class OptionTooltip {
	private static final long DELAY_MS = 350;
	private static final long FADE_MS = 120;
	private static final int MAX_W = 200;
	private static final int LINE_H = 10;

	private final Font font;
	private QuillOption target;
	private long since;

	OptionTooltip(Font font) {
		this.font = font;
	}

	void render(GuiGraphics g, QuillOption hovered, int mx, int my, int screenW, int screenH) {
		long now = Util.getMillis();
		if (hovered != target) {
			target = hovered;
			since = now;
		}
		if (target == null || target.description.isEmpty() || now - since < DELAY_MS) {
			return;
		}
		float alpha = Math.min(1f, (now - since - DELAY_MS) / (float) FADE_MS);
		List<FormattedCharSequence> lines = font.split(Component.literal(target.description), MAX_W);
		String impact = showsImpact(target) ? target.impact.label : null;

		int w = Math.max(SmallCaps.width(font, target.label.trim()), lines.stream().mapToInt(font::width).max().orElse(0));
		if (impact != null) {
			w = Math.max(w, font.width(impact));
		}
		w += 2 * 6;
		int h = 6 + 12 + lines.size() * LINE_H + (impact != null ? LINE_H + 3 : 0) + 4;
		int x = Math.min(mx + 12, screenW - w - 4);
		int y = Math.min(my + 12, screenH - h - 4);
		if (y < my && x < mx) {
			y = my - h - 6;
		}

		g.fill(x + 2, y + 2, x + w + 2, y + h + 2, fade(QuillPalette.DROP_SHADOW, alpha));
		g.fill(x, y, x + w, y + h, fade(QuillPalette.PAPER, alpha));
		g.renderOutline(x, y, w, h, fade(QuillPalette.INK, alpha));
		SmallCaps.draw(g, font, target.label.trim(), x + 6, y + 6, fade(QuillPalette.INK, alpha));
		int ty = y + 18;
		for (FormattedCharSequence line : lines) {
			g.drawString(font, line, x + 6, ty, fade(QuillPalette.INK_SOFT, alpha), false);
			ty += LINE_H;
		}
		if (impact != null) {
			g.hLine(x + 6, x + w - 7, ty + 1, fade(QuillPalette.RULE_FAINT, alpha));
			g.drawString(font, impact, x + 6, ty + 4, fade(target.impact.color, alpha), false);
		}
	}

	private static boolean showsImpact(QuillOption opt) {
		return !(opt instanceof QuillOption.Note) && !(opt instanceof QuillOption.Action);
	}

	private static int fade(int argb, float alpha) {
		int a = Math.round((argb >>> 24) * alpha);
		return a << 24 | argb & 0xFFFFFF;
	}
}
