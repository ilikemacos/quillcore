package com.chopstickshq.quillcore.client.gui;

import com.chopstickshq.quillcore.settings.StagedSettings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Draws the label and control of a single option row. */
final class RowPainter {
	static final int TOGGLE_W = 44;

	private final Font font;
	private final StagedSettings staged;

	RowPainter(Font font, StagedSettings staged) {
		this.font = font;
		this.staged = staged;
	}

	void paint(GuiGraphics g, QuillOption opt, int x, int y, int r, boolean active, float hover, float controlHover, boolean dragging) {
		int textY = y + (QuillLayout.ROW_H - 8) / 2;
		if (opt instanceof QuillOption.Note note) {
			paintNote(g, note, x, textY, r);
			return;
		}
		if (opt.id != null && staged.isPending(opt.id)) {
			g.fill(x + 3, y + 6, x + 5, y + QuillLayout.ROW_H - 6, QuillPalette.OXBLOOD);
		}
		int labelX = x + QuillLayout.PAD + (opt.requires != null ? QuillLayout.PAD : 0) + Math.round(hover);
		g.drawString(font, opt.label.trim(), labelX, textY, active ? QuillPalette.INK : QuillPalette.INK_FAINT, false);

		int cx = r - QuillLayout.PAD - QuillLayout.CONTROL_W;
		int cy = y + (QuillLayout.ROW_H - QuillLayout.CONTROL_H) / 2;
		float t = active ? controlHover : 0f;
		switch (opt) {
			case QuillOption.Toggle toggle -> paintToggle(g, toggle, cx + QuillLayout.CONTROL_W - TOGGLE_W, cy, active, t);
			case QuillOption.Cycle<?> cycle -> paintCycle(g, cycle, cx, cy, active, t);
			case QuillOption.Slider slider -> paintSlider(g, slider, cx, cy, active, dragging ? 1f : t);
			case QuillOption.Action action -> paintAction(g, action, cx, cy, t);
			default -> {
			}
		}
	}

	private void paintNote(GuiGraphics g, QuillOption.Note note, int x, int textY, int r) {
		if (note.heading) {
			SmallCaps.draw(g, font, note.label, x + QuillLayout.PAD, textY + 2, QuillPalette.OXBLOOD);
			int lineX = x + QuillLayout.PAD + SmallCaps.width(font, note.label) + 4;
			g.hLine(lineX, r - QuillLayout.PAD, textY + 6, QuillPalette.RULE);
			return;
		}
		g.drawString(font, note.label, x + QuillLayout.PAD, textY, QuillPalette.INK_SOFT, false);
		int room = r - x - 3 * QuillLayout.PAD - font.width(note.label);
		String value = font.plainSubstrByWidth(note.text.get(), room);
		g.drawString(font, value, r - QuillLayout.PAD - font.width(value), textY, QuillPalette.INK_SOFT, false);
	}

	private void paintToggle(GuiGraphics g, QuillOption.Toggle toggle, int x, int y, boolean active, float t) {
		boolean on = Boolean.TRUE.equals(staged.get(toggle.id)) && active;
		int fill = on ? QuillPalette.INK : QuillPalette.mix(QuillPalette.PAPER_DEEP, QuillPalette.PAPER, t);
		g.fill(x, y, x + TOGGLE_W, y + QuillLayout.CONTROL_H, fill);
		g.renderOutline(x, y, TOGGLE_W, QuillLayout.CONTROL_H, QuillPalette.mix(QuillPalette.RULE, QuillPalette.OXBLOOD, t));
		int knobX = on ? x + TOGGLE_W - 12 : x + 2;
		g.fill(knobX, y + 2, knobX + 10, y + QuillLayout.CONTROL_H - 2, on ? QuillPalette.PAPER : QuillPalette.INK_FAINT);
		String s = on ? "On" : "Off";
		int textX = on ? x + 6 : x + TOGGLE_W - 6 - font.width(s);
		g.drawString(font, s, textX, y + QuillLayout.TEXT_INSET, on ? QuillPalette.PAPER : QuillPalette.INK_SOFT, false);
	}

	private void paintCycle(GuiGraphics g, QuillOption.Cycle<?> cycle, int x, int y, boolean active, float t) {
		int w = QuillLayout.CONTROL_W;
		g.fill(x, y, x + w, y + QuillLayout.CONTROL_H, QuillPalette.mix(QuillPalette.PAPER_DEEP, QuillPalette.PAPER, t));
		g.renderOutline(x, y, w, QuillLayout.CONTROL_H, QuillPalette.mix(QuillPalette.RULE, QuillPalette.OXBLOOD, t));
		int arrow = QuillPalette.mix(QuillPalette.INK_FAINT, QuillPalette.OXBLOOD, t);
		g.drawString(font, "‹", x + 4, y + QuillLayout.TEXT_INSET, arrow, false);
		g.drawString(font, "›", x + w - 7, y + QuillLayout.TEXT_INSET, arrow, false);
		String s = font.plainSubstrByWidth(cycle.text(staged.get(cycle.id)), w - 20);
		g.drawString(font, s, x + (w - font.width(s)) / 2, y + QuillLayout.TEXT_INSET, active ? QuillPalette.INK : QuillPalette.INK_FAINT, false);
	}

	private void paintSlider(GuiGraphics g, QuillOption.Slider slider, int x, int y, boolean active, float t) {
		int w = QuillLayout.CONTROL_W;
		int value = staged.<Integer>get(slider.id);
		int knob = x + (int) Math.round((w - 4) * slider.fraction(value));
		int ink = active ? QuillPalette.INK : QuillPalette.INK_FAINT;
		int trackY = y + QuillLayout.CONTROL_H / 2 - 1;
		g.fill(x, trackY, x + w, trackY + 3, QuillPalette.PAPER_DEEP);
		g.fill(x, trackY, knob, trackY + 3, active ? QuillPalette.mix(QuillPalette.RULE, QuillPalette.OXBLOOD, t) : QuillPalette.RULE_FAINT);
		g.renderOutline(x, trackY, w, 3, QuillPalette.RULE);
		g.fill(knob, y + 1, knob + 4, y + QuillLayout.CONTROL_H - 1, active ? QuillPalette.mix(QuillPalette.INK, QuillPalette.OXBLOOD, t) : ink);
		String s = font.plainSubstrByWidth(slider.format.apply(value), QuillLayout.VALUE_W);
		g.drawString(font, s, x - 6 - font.width(s), y + QuillLayout.TEXT_INSET, ink, false);
	}

	private void paintAction(GuiGraphics g, QuillOption.Action action, int x, int y, float t) {
		int w = QuillLayout.CONTROL_W;
		g.fill(x, y, x + w, y + QuillLayout.CONTROL_H, QuillPalette.mix(QuillPalette.PAPER_DEEP, QuillPalette.INK, t));
		g.renderOutline(x, y, w, QuillLayout.CONTROL_H, QuillPalette.mix(QuillPalette.RULE, QuillPalette.INK, t));
		String s = action.button + " ›";
		g.drawString(font, s, x + (w - font.width(s)) / 2, y + QuillLayout.TEXT_INSET, QuillPalette.mix(QuillPalette.INK, QuillPalette.PAPER, t), false);
	}
}
