package com.chopstickshq.quillcore.client.gui;

import com.chopstickshq.quillcore.settings.StagedSettings;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Scrollable column of option rows for the current page. */
final class OptionList {
	private static final float SCROLL_EASE = 0.4f;

	private record ControlKey(QuillOption option) {}

	private final StagedSettings staged;
	private final RowPainter painter;
	private List<QuillOption> rows = List.of();
	private int x, y, r, b;
	private double scrollTarget;
	private double scroll;
	private QuillOption.Slider dragging;

	OptionList(Font font, StagedSettings staged) {
		this.staged = staged;
		this.painter = new RowPainter(font, staged);
	}

	void place(int x, int y, int r, int b) {
		this.x = x;
		this.y = y;
		this.r = r;
		this.b = b;
		clampScroll();
		scroll = scrollTarget;
	}

	void show(List<QuillOption> rows) {
		this.rows = rows;
		scrollTarget = 0;
		scroll = 0;
		dragging = null;
	}

	boolean contains(double mx, double my) {
		return mx >= x && mx < r && my >= y && my < b;
	}

	/** Draws the rows and returns the one under the cursor, if any. */
	QuillOption render(GuiGraphics g, HoverFade fade, int mx, int my, boolean interactive) {
		scroll += (scrollTarget - scroll) * SCROLL_EASE;
		if (Math.abs(scrollTarget - scroll) < 0.5) {
			scroll = scrollTarget;
		}
		QuillOption hovered = null;
		g.enableScissor(x, y, r, b);
		for (int i = 0; i < rows.size(); i++) {
			int ry = rowY(i);
			QuillOption opt = rows.get(i);
			if (ry + QuillLayout.ROW_H < y || ry > b) {
				fade.update(opt, false);
				continue;
			}
			boolean active = isActive(opt);
			boolean hover = interactive && contains(mx, my) && my >= ry && my < ry + QuillLayout.ROW_H && isHoverable(opt);
			float t = fade.update(opt, hover);
			float controlT = fade.update(new ControlKey(opt), hover && overControl(opt, mx));
			if (t > 0f) {
				g.fill(x, ry, r - 4, ry + QuillLayout.ROW_H, QuillPalette.mix(QuillPalette.PAPER, QuillPalette.PAPER_HOVER, t));
			}
			if (hover) {
				hovered = opt;
			}
			painter.paint(g, opt, x, ry, r, active, t, controlT, dragging == opt);
			g.hLine(x + QuillLayout.PAD, r - QuillLayout.PAD, ry + QuillLayout.ROW_H - 1, QuillPalette.RULE_FAINT);
		}
		g.disableScissor();
		renderScrollbar(g);
		return hovered;
	}

	private void renderScrollbar(GuiGraphics g) {
		int content = contentHeight();
		int view = b - y;
		if (content <= view) {
			return;
		}
		int barH = Math.max(12, view * view / content);
		int barY = y + (int) ((view - barH) * (scroll / (content - view)));
		g.fill(r - 3, barY, r - 1, barY + barH, QuillPalette.INK_FAINT);
	}

	boolean click(double mx, double my, int button) {
		if (!contains(mx, my)) {
			return false;
		}
		QuillOption opt = rowAt(my);
		if (opt == null || !isActive(opt)) {
			return true;
		}
		switch (opt) {
			case QuillOption.Toggle toggle -> staged.stage(toggle.id, !Boolean.TRUE.equals(staged.get(toggle.id)));
			case QuillOption.Cycle<?> cycle -> staged.stage(cycle.id, cycle.step(staged.get(cycle.id), button == 1 ? -1 : 1));
			case QuillOption.Slider slider when mx >= controlX() -> {
				dragging = slider;
				dragTo(mx);
			}
			case QuillOption.Action action when mx >= controlX() -> action.run.run();
			default -> {
			}
		}
		return true;
	}

	boolean drag(double mx) {
		if (dragging == null) {
			return false;
		}
		dragTo(mx);
		return true;
	}

	void release() {
		dragging = null;
	}

	boolean scroll(double mx, double my, double amount) {
		if (!contains(mx, my)) {
			return false;
		}
		scrollTarget -= amount * QuillLayout.ROW_H;
		clampScroll();
		return true;
	}

	private void dragTo(double mx) {
		staged.stage(dragging.id, dragging.snap((mx - controlX()) / QuillLayout.CONTROL_W));
	}

	private int controlX() {
		return r - QuillLayout.PAD - QuillLayout.CONTROL_W;
	}

	private boolean overControl(QuillOption opt, int mx) {
		if (opt instanceof QuillOption.Toggle) {
			return mx >= r - QuillLayout.PAD - RowPainter.TOGGLE_W && mx < r - QuillLayout.PAD;
		}
		return mx >= controlX() && mx < r - QuillLayout.PAD;
	}

	private int rowY(int i) {
		return y + 4 + i * QuillLayout.ROW_H - (int) Math.round(scroll);
	}

	private QuillOption rowAt(double my) {
		int i = (int) Math.floor((my - y - 4 + scroll) / QuillLayout.ROW_H);
		return i >= 0 && i < rows.size() ? rows.get(i) : null;
	}

	private boolean isActive(QuillOption opt) {
		if (opt instanceof QuillOption.Note) {
			return false;
		}
		return opt.requires == null || Boolean.TRUE.equals(staged.get(opt.requires));
	}

	private static boolean isHoverable(QuillOption opt) {
		return !(opt instanceof QuillOption.Note note && note.heading);
	}

	private int contentHeight() {
		return rows.size() * QuillLayout.ROW_H + 8;
	}

	private void clampScroll() {
		scrollTarget = Math.max(0, Math.min(scrollTarget, Math.max(0, contentHeight() - (b - y))));
	}
}
