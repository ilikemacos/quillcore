package com.chopstickshq.quillcore.client.gui;

import com.chopstickshq.quillcore.client.QuillCoreConfigScreen;
import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.settings.StagedSettings;
import com.chopstickshq.quillcore.settings.SupportPromptPolicy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** QuillCore video settings: page tabs, staged option rows with tooltips, and Undo / Apply / Done. */
public final class QuillVideoSettingsScreen extends Screen {
	private static final String COFFEE_LABEL = "Buy me a coffee";
	private static final int FOOTER_BUTTON_W = 60;

	private static boolean vanillaPassThrough;

	private final Screen parent;
	private final StagedSettings staged = new StagedSettings();
	private final HoverFade fade = new HoverFade();
	private final List<FlatButton> footer = new ArrayList<>();
	private final boolean promptCoffee;

	private List<OptionPage> pages;
	private TabRail tabs;
	private OptionList list;
	private OptionTooltip tooltip;
	private SupportCard supportCard;
	private int left, top, right, bottom;

	public QuillVideoSettingsScreen(Screen parent) {
		super(Component.literal("QuillCore Video Settings"));
		this.parent = parent;
		this.promptCoffee = recordOpen();
	}

	/** True once after {@link #openVanilla()} so the redirect mixin lets the vanilla screen through. */
	public static boolean consumeVanillaPassThrough() {
		boolean v = vanillaPassThrough;
		vanillaPassThrough = false;
		return v;
	}

	private static boolean recordOpen() {
		QuillConfig c = QuillConfig.get();
		SupportPromptPolicy.State s = SupportPromptPolicy.recordOpen(
				new SupportPromptPolicy.State(c.bmcOpenCount, c.bmcLastShownEpochMs, c.bmcNeverShow));
		long now = System.currentTimeMillis();
		boolean show = SupportPromptPolicy.shouldShow(s, now);
		if (show) {
			s = SupportPromptPolicy.recordShown(s, now);
		}
		c.bmcOpenCount = s.openCount();
		c.bmcLastShownEpochMs = s.lastShownEpochMs();
		QuillConfig.save();
		return show;
	}

	@Override
	protected void init() {
		if (pages == null) {
			pages = new VideoOptionCatalog(minecraft, staged, this::openVanilla,
					() -> minecraft.setScreen(QuillCoreConfigScreen.create(this))).build();
			tabs = new TabRail(pages);
			list = new OptionList(font, staged);
			list.show(pages.get(0).options());
			tooltip = new OptionTooltip(font);
			supportCard = new SupportCard(font, promptCoffee, this::openSupportLink, QuillVideoSettingsScreen::neverPromptAgain);
		}
		left = QuillLayout.MARGIN;
		top = QuillLayout.MARGIN;
		right = width - QuillLayout.MARGIN;
		bottom = height - QuillLayout.MARGIN;

		int bodyTop = top + QuillLayout.HEADER_H;
		int bodyBottom = bottom - QuillLayout.FOOTER_H;
		int listX = left + QuillLayout.TAB_W + 1;
		tabs.place(left + 1, bodyTop, bodyBottom);
		list.place(listX, bodyTop, right - 1, bodyBottom);
		supportCard.place(listX, bodyTop, right - 1, bodyBottom);
		layoutFooter();
	}

	private void layoutFooter() {
		footer.clear();
		int y = bottom - QuillLayout.FOOTER_H + (QuillLayout.FOOTER_H - QuillLayout.BUTTON_H) / 2;
		footer.add(FlatButton.coffee(font, left + QuillLayout.PAD, y, COFFEE_LABEL, this::openSupportLink));
		int x = right - QuillLayout.PAD - FOOTER_BUTTON_W;
		footer.add(FlatButton.text(x, y, FOOTER_BUTTON_W, "Done", () -> true, this::applyAndClose));
		x -= FOOTER_BUTTON_W + 4;
		footer.add(FlatButton.text(x, y, FOOTER_BUTTON_W, "Apply", staged::hasPending, this::applyPending));
		x -= FOOTER_BUTTON_W + 4;
		footer.add(FlatButton.text(x, y, FOOTER_BUTTON_W, "Undo", staged::hasPending, staged::undo));
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private void applyAndClose() {
		applyPending();
		onClose();
	}

	private void openVanilla() {
		vanillaPassThrough = true;
		minecraft.setScreen(new VideoSettingsScreen(this, minecraft, minecraft.options));
	}

	private void openSupportLink() {
		String url = QuillConfig.get().supportUrl;
		minecraft.setScreen(new ConfirmLinkScreen(open -> {
			if (open) {
				Util.getPlatform().openUri(url);
			}
			minecraft.setScreen(this);
		}, url, true));
	}

	private static void neverPromptAgain() {
		QuillConfig.get().bmcNeverShow = true;
		QuillConfig.save();
	}

	private void applyPending() {
		if (!staged.hasPending()) {
			return;
		}
		Integer mip = staged.isPending("mipmap") ? staged.get("mipmap") : null;
		List<String> applied = staged.apply();
		QuillConfig.save();
		minecraft.options.save();
		if (mip != null) {
			minecraft.updateMaxMipLevel(mip);
			minecraft.delayTextureReload();
		}
		if (applied.contains("guiScale")) {
			minecraft.resizeDisplay();
		}
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		fade.tick();
		boolean interactive = !supportCard.isOpen();
		g.fill(0, 0, width, height, QuillPalette.SHADE);
		g.fill(left, top, right, bottom, QuillPalette.PAPER);
		g.renderOutline(left, top, right - left, bottom - top, QuillPalette.INK);

		renderHeader(g);
		tabs.render(g, font, fade, mx, my, interactive);
		QuillOption hovered = list.render(g, fade, mx, my, interactive);
		g.hLine(left, right - 1, bottom - QuillLayout.FOOTER_H, QuillPalette.INK);
		for (FlatButton b : footer) {
			b.render(g, font, fade, mx, my, interactive || b == footer.get(0));
		}
		supportCard.render(g, fade, mx, my);
		tooltip.render(g, interactive ? hovered : null, mx, my, width, height);
	}

	private void renderHeader(GuiGraphics g) {
		int y = top + (QuillLayout.HEADER_H - 8) / 2;
		int x = left + QuillLayout.PAD;
		SmallCaps.draw(g, font, "QuillCore", x, y, QuillPalette.OXBLOOD);
		x += SmallCaps.width(font, "QuillCore") + 4;
		SmallCaps.draw(g, font, "· Video settings", x, y, QuillPalette.INK);
		if (staged.hasPending()) {
			String pending = staged.pendingCount() + " unapplied";
			g.drawString(font, pending, right - QuillLayout.PAD - font.width(pending), y, QuillPalette.OXBLOOD, false);
		}
		g.hLine(left, right - 1, top + QuillLayout.HEADER_H - 1, QuillPalette.INK);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (footer.get(0).click(mx, my) || supportCard.click(mx, my)) {
			return true;
		}
		for (FlatButton b : footer) {
			if (b.click(mx, my)) {
				return true;
			}
		}
		if (tabs.click(mx, my)) {
			list.show(pages.get(tabs.selected()).options());
			return true;
		}
		return tabs.contains(mx, my) || list.click(mx, my, button) || super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		return list.drag(mx) || super.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		list.release();
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		return list.scroll(mx, my, v) || super.mouseScrolled(mx, my, h, v);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (key == GLFW.GLFW_KEY_ESCAPE && supportCard.isOpen()) {
			supportCard.dismiss();
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			applyPending();
			return true;
		}
		return super.keyPressed(key, scan, mods);
	}
}
