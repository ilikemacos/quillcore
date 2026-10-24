package com.chopstickshq.quillcore.client.gui;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Vertical list of page tabs with a marker that glides to the selected one. */
final class TabRail {
	private static final float GLIDE = 0.35f;

	private final List<OptionPage> pages;
	private int x, y, bottom;
	private int selected;
	private float markerY = Float.NaN;

	TabRail(List<OptionPage> pages) {
		this.pages = pages;
	}

	void place(int x, int y, int bottom) {
		this.x = x;
		this.y = y;
		this.bottom = bottom;
		markerY = Float.NaN;
	}

	int selected() {
		return selected;
	}

	boolean click(double mx, double my) {
		for (int i = 0; i < pages.size(); i++) {
			if (hit(i, mx, my)) {
				boolean changed = i != selected;
				selected = i;
				return changed;
			}
		}
		return false;
	}

	boolean contains(double mx, double my) {
		return mx >= x && mx < x + QuillLayout.TAB_W && my >= y && my < bottom;
	}

	void render(GuiGraphics g, Font font, HoverFade fade, int mx, int my, boolean interactive) {
		int r = x + QuillLayout.TAB_W;
		g.fill(x, y, r, bottom, QuillPalette.PAPER_DEEP);
		g.vLine(r, y - 1, bottom, QuillPalette.RULE);

		float target = tabY(selected);
		markerY = Float.isNaN(markerY) ? target : markerY + (target - markerY) * GLIDE;
		if (Math.abs(target - markerY) < 0.5f) {
			markerY = target;
		}
		int my0 = Math.round(markerY);
		g.fill(x, my0, r + 1, my0 + QuillLayout.TAB_H, QuillPalette.PAPER);
		g.hLine(x, r, my0, QuillPalette.RULE);
		g.hLine(x, r, my0 + QuillLayout.TAB_H - 1, QuillPalette.RULE);
		g.fill(x, my0, x + 2, my0 + QuillLayout.TAB_H, QuillPalette.OXBLOOD);

		for (int i = 0; i < pages.size(); i++) {
			int ty = tabY(i);
			boolean sel = i == selected;
			float t = fade.update(pages.get(i), interactive && !sel && hit(i, mx, my));
			if (t > 0f) {
				g.fill(x, ty, r, ty + QuillLayout.TAB_H, QuillPalette.mix(QuillPalette.PAPER_DEEP, QuillPalette.PAPER_HOVER, t));
			}
			int color = sel ? QuillPalette.INK : QuillPalette.mix(QuillPalette.INK_SOFT, QuillPalette.INK, t);
			int indent = sel ? 0 : Math.round(t * 2);
			g.drawString(font, pages.get(i).title(), x + QuillLayout.PAD + indent, ty + (QuillLayout.TAB_H - 8) / 2, color, false);
		}
	}

	private int tabY(int i) {
		return y + 6 + i * QuillLayout.TAB_H;
	}

	private boolean hit(int i, double mx, double my) {
		int ty = tabY(i);
		return mx >= x && mx < x + QuillLayout.TAB_W && my >= ty && my < ty + QuillLayout.TAB_H;
	}
}
