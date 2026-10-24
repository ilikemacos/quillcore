package com.chopstickshq.quillcore.client.gui;

import com.chopstickshq.quillcore.client.QuillCoreConfigScreen;
import com.chopstickshq.quillcore.client.gui.QuillOption.Impact;
import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.settings.StagedSettings;
import com.chopstickshq.quillcore.settings.SupportPromptPolicy;
import com.chopstickshq.quillcore.upscale.fsr.FsrController;
import com.chopstickshq.quillcore.upscale.fsr.FsrQualityPreset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.OptionEnum;
import org.lwjgl.glfw.GLFW;

/**
 * QuillCore video settings: category tabs on the left, staged option rows on the right,
 * a description panel for the hovered row, and Undo / Apply / Done. Replaces vanilla Video Settings.
 */
public final class QuillVideoSettingsScreen extends Screen {
	private static final int MARGIN = 8;
	private static final int HEADER_H = 22;
	private static final int FOOTER_H = 26;
	private static final int TAB_W = 100;
	private static final int TAB_H = 18;
	private static final int ROW_H = 20;
	private static final int DESC_H = 46;
	private static final int CTRL_W = 112;
	private static final int SLIDER_VALUE_W = 64;

	private static boolean vanillaPassThrough;

	private final Screen parent;
	private final StagedSettings staged = new StagedSettings();
	private final List<String> tabNames = List.of("General", "Quality", "Performance", "Upscaling (FSR)", "Advanced");
	private final List<List<QuillOption>> tabs = new ArrayList<>();
	private final List<FooterButton> footer = new ArrayList<>();
	private final List<FooterButton> cardButtons = new ArrayList<>();

	private int tab;
	private double scroll;
	private QuillOption.Slider dragging;
	private QuillOption described;
	private boolean showSupportCard;

	// Layout, recomputed in init().
	private int left, top, right, bottom, listX, listY, listR, listB;

	public QuillVideoSettingsScreen(Screen parent) {
		super(Component.literal("QuillCore Video Settings"));
		this.parent = parent;
		recordOpenForSupportCard();
	}

	/** True once after {@link #openVanilla()} so the redirect mixin lets the vanilla screen through. */
	public static boolean consumeVanillaPassThrough() {
		boolean v = vanillaPassThrough;
		vanillaPassThrough = false;
		return v;
	}

	private void recordOpenForSupportCard() {
		QuillConfig c = QuillConfig.get();
		SupportPromptPolicy.State s = SupportPromptPolicy.recordOpen(
				new SupportPromptPolicy.State(c.bmcOpenCount, c.bmcLastShownEpochMs, c.bmcNeverShow));
		long now = System.currentTimeMillis();
		if (SupportPromptPolicy.shouldShow(s, now)) {
			showSupportCard = true;
			s = SupportPromptPolicy.recordShown(s, now);
		}
		c.bmcOpenCount = s.openCount();
		c.bmcLastShownEpochMs = s.lastShownEpochMs();
		QuillConfig.save();
	}

	// options

	private void buildOptions() {
		if (!tabs.isEmpty()) {
			return;
		}
		Options o = minecraft.options;

		List<QuillOption> general = new ArrayList<>();
		general.add(vanillaSlider("renderDistance", o.renderDistance(), 2, 32, "Render distance", Impact.HIGH,
				"How many chunks around you are drawn. The single biggest lever on frame rate and memory.", v -> v + " chunks"));
		general.add(vanillaSlider("simulationDistance", o.simulationDistance(), 5, 32, "Simulation distance", Impact.HIGH,
				"How far away entities and blocks keep ticking in singleplayer. Servers decide this for themselves.", v -> v + " chunks"));
		bind("maxFps", o.framerateLimit());
		general.add(new QuillOption.Slider("maxFps", "Max framerate", "Caps frames per second. Lower caps save power and heat; 'Unlimited' runs as fast as possible.",
				Impact.MEDIUM, 10, 260, 10, v -> v >= 260 ? "Unlimited" : v + " fps"));
		bind("vsync", o.enableVsync());
		general.add(new QuillOption.Toggle("vsync", "VSync", "Syncs frames to the display refresh. Removes tearing, may add a little input latency.", Impact.LOW));
		bind("fullscreen", o.fullscreen());
		general.add(new QuillOption.Toggle("fullscreen", "Fullscreen", "Run the game in exclusive fullscreen.", Impact.NONE));
		general.add(vanillaSlider("guiScale", o.guiScale(), 0, Math.max(1, minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode())),
				"GUI scale", Impact.NONE, "Size of menus and the HUD. 'Auto' picks the largest size that fits.", v -> v == 0 ? "Auto" : v + "x"));
		bindDouble("brightness", o.gamma(), 100.0);
		general.add(new QuillOption.Slider("brightness", "Brightness", "Gamma. Purely visual.", Impact.NONE, 0, 100, 5,
				v -> v == 0 ? "Moody" : v == 100 ? "Bright" : v + "%"));
		bind("bobView", o.bobView());
		general.add(new QuillOption.Toggle("bobView", "View bobbing", "Camera sway while walking.", Impact.NONE));
		general.add(vanillaCycle("inactivityFps", o.inactivityFpsLimit(), "Unfocused framerate", Impact.SAVES,
				"Lowers the frame rate while the window is unfocused or minimized."));
		bindConfig("masterEnabled", () -> QuillConfig.get().masterEnabled, v -> QuillConfig.get().masterEnabled = v);
		general.add(new QuillOption.Toggle("masterEnabled", "QuillCore enabled", "Master switch. Off turns every QuillCore optimization off at once.", Impact.SAVES));
		tabs.add(general);

		List<QuillOption> quality = new ArrayList<>();
		quality.add(vanillaCycle("graphics", o.graphicsMode(), "Graphics", Impact.HIGH,
				"Fast skips transparent leaves and some effects. Fabulous adds translucency sorting and costs the most."));
		quality.add(vanillaCycle("clouds", o.cloudStatus(), "Clouds", Impact.MEDIUM, "Cloud style, or none at all."));
		quality.add(vanillaSlider("cloudRange", o.cloudRange(), 2, 128, "Cloud distance", Impact.LOW,
				"How far clouds are drawn (vanilla). QuillCore can cap this further under Performance.", v -> v + " chunks"));
		bind("ao", o.ambientOcclusion());
		quality.add(new QuillOption.Toggle("ao", "Smooth lighting", "Soft shading in block corners.", Impact.LOW));
		bind("shadows", o.entityShadows());
		quality.add(new QuillOption.Toggle("shadows", "Entity shadows", "Round shadows under mobs and items.", Impact.LOW));
		bindDouble("entityDistance", o.entityDistanceScaling(), 4.0);
		quality.add(new QuillOption.Slider("entityDistance", "Entity distance", "Vanilla scale for how far entities stay visible.",
				Impact.MEDIUM, 2, 20, 1, v -> (v * 25) + "%"));
		quality.add(vanillaSlider("biomeBlend", o.biomeBlendRadius(), 0, 7, "Biome blend", Impact.MEDIUM,
				"Smooths grass/water colour between biomes. Higher values slow chunk building.", v -> v == 0 ? "Off" : (v * 2 + 1) + "x" + (v * 2 + 1)));
		quality.add(vanillaSlider("mipmap", o.mipmapLevels(), 0, 4, "Mipmap levels", Impact.LOW,
				"Smooths distant textures. Applying reloads textures.", v -> v == 0 ? "Off" : String.valueOf(v)));
		quality.add(vanillaCycle("chunkUpdates", o.prioritizeChunkUpdates(), "Chunk builder", Impact.MEDIUM,
				"Which chunk rebuilds happen on the main thread. Blocking options reduce visible holes but can stutter."));
		tabs.add(quality);

		List<QuillOption> perf = new ArrayList<>();
		perf.add(new QuillOption.Note("Quill Render", true, () -> "", ""));
		bindConfig("renderEnabled", () -> QuillConfig.get().renderEnabled, v -> QuillConfig.get().renderEnabled = v);
		perf.add(new QuillOption.Toggle("renderEnabled", "Quill Render", "All client-side render cutoffs and caps below.", Impact.SAVES));
		cutoff(perf, "entityCutoff", "Entity render cutoff", "Skip drawing entities beyond this distance. Camera and your vehicle are exempt.",
				() -> QuillConfig.get().entityRenderCutoffEnabled, v -> QuillConfig.get().entityRenderCutoffEnabled = v,
				() -> QuillConfig.get().entityRenderCutoffBlocks, v -> QuillConfig.get().entityRenderCutoffBlocks = v);
		cutoff(perf, "beCutoff", "Block entity cutoff", "Skip distant signs and skulls. Chests and animated block entities are never skipped.",
				() -> QuillConfig.get().blockEntityRenderCutoffEnabled, v -> QuillConfig.get().blockEntityRenderCutoffEnabled = v,
				() -> QuillConfig.get().blockEntityRenderCutoffBlocks, v -> QuillConfig.get().blockEntityRenderCutoffBlocks = v);
		cutoff(perf, "particleCutoff", "Particle cutoff", "Don't spawn particles beyond this distance from the camera. Visual only.",
				() -> QuillConfig.get().particleRenderCutoffEnabled, v -> QuillConfig.get().particleRenderCutoffEnabled = v,
				() -> QuillConfig.get().particleRenderCutoffBlocks, v -> QuillConfig.get().particleRenderCutoffBlocks = v);
		bindConfig("cloudCap", () -> QuillConfig.get().cloudDistanceCapEnabled, v -> QuillConfig.get().cloudDistanceCapEnabled = v);
		perf.add(new QuillOption.Toggle("cloudCap", "Cloud distance cap", "Caps the vanilla cloud distance further. Off by default.", Impact.SAVES));
		bindConfig("cloudCapChunks", () -> QuillConfig.get().cloudDistanceCapChunks, v -> QuillConfig.get().cloudDistanceCapChunks = v);
		perf.add(new QuillOption.Slider("cloudCapChunks", "  Cap", "Maximum cloud distance while the cap is on.", Impact.SAVES, 1, 128, 1, v -> v + " chunks").requires("cloudCap"));
		bindConfig("weatherCap", () -> QuillConfig.get().weatherRadiusCapEnabled, v -> QuillConfig.get().weatherRadiusCapEnabled = v);
		perf.add(new QuillOption.Toggle("weatherCap", "Weather radius cap", "Caps the rain/snow column radius. Low values show a visible edge.", Impact.SAVES));
		bindConfig("weatherCapBlocks", () -> QuillConfig.get().weatherRadiusCapBlocks, v -> QuillConfig.get().weatherRadiusCapBlocks = v);
		perf.add(new QuillOption.Slider("weatherCapBlocks", "  Radius", "Rain/snow radius while the cap is on.", Impact.SAVES, 1, 10, 1, v -> v + " blocks").requires("weatherCap"));
		perf.add(new QuillOption.Note("Quill Logic", true, () -> "", ""));
		bindConfig("logicEnabled", () -> QuillConfig.get().logicEnabled, v -> QuillConfig.get().logicEnabled = v);
		perf.add(new QuillOption.Toggle("logicEnabled", "Quill Logic", "Server-side tick throttles for idle, far-away mobs and items.", Impact.SAVES));
		logic(perf, "idleAi", "Idle AI throttle", "Skip goal ticks for idle mobs far from players. Sensing and movement still run.",
				() -> QuillConfig.get().idleAiThrottleEnabled, v -> QuillConfig.get().idleAiThrottleEnabled = v);
		logic(perf, "brainAi", "Villager brain throttle", "Skip brain ticks for idle distant villagers, never while trading, panicking or hurt.",
				() -> QuillConfig.get().brainIdleThrottleEnabled, v -> QuillConfig.get().brainIdleThrottleEnabled = v);
		logic(perf, "itemMerge", "Item/XP merge throttle", "Fewer neighbour-merge scans for far items and orbs. Pickup and despawn timing untouched.",
				() -> QuillConfig.get().itemMergeThrottleEnabled, v -> QuillConfig.get().itemMergeThrottleEnabled = v);
		logic(perf, "pathNav", "Path navigation throttle", "Skip navigation ticks for far idle mobs with no active path.",
				() -> QuillConfig.get().pathNavThrottleEnabled, v -> QuillConfig.get().pathNavThrottleEnabled = v);
		tabs.add(perf);

		List<QuillOption> fsr = new ArrayList<>();
		bindConfig("fsr", () -> "fsr1".equals(QuillConfig.get().upscaleBackendId), v -> QuillConfig.get().upscaleBackendId = v ? "fsr1" : "none");
		fsr.add(new QuillOption.Toggle("fsr", "FSR 1 upscaling", "Renders the world at a lower resolution and upscales it with AMD FSR 1 (EASU + RCAS). The HUD stays native.", Impact.SAVES));
		bindConfig("fsrPreset", () -> FsrQualityPreset.fromId(QuillConfig.get().fsrQualityPreset), v -> QuillConfig.get().fsrQualityPreset = v.name());
		fsr.add(new QuillOption.Cycle<>("fsrPreset", "Quality preset", "Internal render scale. Ultra Quality is sharpest, Ultra Performance is fastest.",
				Impact.SAVES, Arrays.asList(FsrQualityPreset.values()), p -> p.label() + " (" + Math.round(p.renderScale() * 100) + "%)").requires("fsr"));
		bindConfig("fsrSharpness", () -> Math.round(QuillConfig.get().fsrSharpness * 10), v -> QuillConfig.get().fsrSharpness = v / 10f);
		fsr.add(new QuillOption.Slider("fsrSharpness", "Sharpness", "RCAS sharpening. 0 is the sharpest, 2 the softest.", Impact.LOW, 0, 20, 1,
				v -> String.format("%.1f", v / 10f)).requires("fsr"));
		bindConfig("fsrScale", () -> Math.round(QuillConfig.get().fsrRenderScaleOverride * 100), v -> QuillConfig.get().fsrRenderScaleOverride = v / 100f);
		fsr.add(new QuillOption.Slider("fsrScale", "Render scale override", "Manual internal scale. 'Preset' follows the quality preset; 100% disables upscaling.",
				Impact.SAVES, 0, 100, 5, v -> v == 0 ? "Preset" : Math.max(25, v) + "%").requires("fsr"));
		fsr.add(new QuillOption.Note("Status", false, FsrController::runtimeStatus, "Live state of the upscaler."));
		tabs.add(fsr);

		List<QuillOption> adv = new ArrayList<>();
		bindConfig("bench", () -> QuillConfig.get().benchEnabled, v -> QuillConfig.get().benchEnabled = v);
		adv.add(new QuillOption.Toggle("bench", "Bench sampling", "Sample server ticks and client frames for /quillcore bench report.", Impact.LOW));
		adv.add(new QuillOption.Action("Vanilla video settings", "Open Minecraft's original Video Settings screen.", "Open", this::openVanilla));
		if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
			adv.add(new QuillOption.Action("All QuillCore options", "Every tunable, including throttle radii and skip ticks (Cloth Config).", "Open",
					() -> minecraft.setScreen(QuillCoreConfigScreen.create(this))));
		}
		adv.add(new QuillOption.Note("Config file", false, () -> "config/quillcore.json", "Where these settings are stored."));
		tabs.add(adv);
	}

	private void cutoff(List<QuillOption> list, String id, String label, String desc,
			Supplier<Boolean> enabled, Consumer<Boolean> setEnabled, Supplier<Double> blocks, Consumer<Double> setBlocks) {
		bindConfig(id, enabled, setEnabled);
		list.add(new QuillOption.Toggle(id, label, desc, Impact.SAVES));
		bindConfig(id + "Blocks", () -> (int) Math.round(blocks.get()), v -> setBlocks.accept((double) (int) v));
		list.add(new QuillOption.Slider(id + "Blocks", "  Distance", desc, Impact.SAVES, 16, 512, 8, v -> v + " blocks").requires(id));
	}

	private void logic(List<QuillOption> list, String id, String label, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
		bindConfig(id, get, set);
		list.add(new QuillOption.Toggle(id, label, desc, Impact.SAVES).requires("logicEnabled"));
	}

	private <T> void bindConfig(String id, Supplier<T> get, Consumer<T> set) {
		staged.bind(id, get, set);
	}

	private <T> void bind(String id, OptionInstance<T> option) {
		staged.bind(id, option::get, option::set);
	}

	private void bindDouble(String id, OptionInstance<Double> option, double scale) {
		staged.bind(id, () -> (int) Math.round(option.get() * scale), (Integer v) -> option.set(v / scale));
	}

	private QuillOption vanillaSlider(String id, OptionInstance<Integer> option, int min, int max, String label, Impact impact,
			String desc, java.util.function.IntFunction<String> fmt) {
		if (option.values() instanceof OptionInstance.IntRange r) {
			min = r.minInclusive();
			max = r.maxInclusive();
		}
		bind(id, option);
		return new QuillOption.Slider(id, label, desc, impact, min, max, 1, fmt);
	}

	private <T extends Enum<T> & OptionEnum> QuillOption vanillaCycle(String id, OptionInstance<T> option, String label, Impact impact, String desc) {
		bind(id, option);
		List<T> values = Arrays.asList(option.get().getDeclaringClass().getEnumConstants());
		return new QuillOption.Cycle<>(id, label, desc, impact, values, v -> v.getCaption().getString());
	}

	// lifecycle

	@Override
	protected void init() {
		buildOptions();
		left = MARGIN;
		top = MARGIN;
		right = width - MARGIN;
		bottom = height - MARGIN;
		listX = left + TAB_W + 1;
		listY = top + HEADER_H;
		listR = right - 1;
		listB = bottom - FOOTER_H - DESC_H;
		clampScroll();

		footer.clear();
		int by = bottom - FOOTER_H + 5;
		footer.add(new FooterButton(left + 6, by, font.width("Buy me a coffee") + 22, 16, "Buy me a coffee", true, () -> true, this::openSupportLink));
		int bx = right - 6;
		bx -= 60;
		footer.add(new FooterButton(bx, by, 60, 16, "Done", false, () -> true, () -> {
			applyPending();
			onClose();
		}));
		bx -= 64;
		footer.add(new FooterButton(bx, by, 60, 16, "Apply", false, staged::hasPending, this::applyPending));
		bx -= 64;
		footer.add(new FooterButton(bx, by, 60, 16, "Undo", false, staged::hasPending, staged::undo));

		cardButtons.clear();
		int cw = cardW(), cx = (listX + listR - cw) / 2, cy = cardY();
		int y = cy + cardH() - 22;
		int w3 = (cw - 24) / 3;
		cardButtons.add(new FooterButton(cx + 6, y, w3, 16, "Buy a coffee", false, () -> true, () -> {
			showSupportCard = false;
			openSupportLink();
		}));
		cardButtons.add(new FooterButton(cx + 12 + w3, y, w3, 16, "Not now", false, () -> true, () -> showSupportCard = false));
		cardButtons.add(new FooterButton(cx + 18 + 2 * w3, y, w3, 16, "Don't show again", false, () -> true, () -> {
			showSupportCard = false;
			QuillConfig.get().bmcNeverShow = true;
			QuillConfig.save();
		}));
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
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

	// rendering

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, QuillPalette.SHADE);
		g.fill(left, top, right, bottom, QuillPalette.PAPER);
		g.renderOutline(left, top, right - left, bottom - top, QuillPalette.INK);

		// Header
		smallCaps(g, "QuillCore", left + 8, top + 7, QuillPalette.OXBLOOD);
		smallCaps(g, "· Video settings", left + 8 + smallCapsWidth("QuillCore") + 4, top + 7, QuillPalette.INK);
		if (staged.hasPending()) {
			String p = staged.pendingCount() + " unapplied";
			g.drawString(font, p, right - 8 - font.width(p), top + 7, QuillPalette.OXBLOOD, false);
		}
		g.hLine(left, right - 1, top + HEADER_H - 1, QuillPalette.INK);

		// Tabs
		g.fill(left + 1, top + HEADER_H, left + TAB_W, bottom - FOOTER_H, QuillPalette.PAPER_DEEP);
		g.vLine(left + TAB_W, top + HEADER_H - 1, bottom - FOOTER_H, QuillPalette.RULE);
		for (int i = 0; i < tabNames.size(); i++) {
			int ty = top + HEADER_H + 4 + i * TAB_H;
			boolean sel = i == tab;
			boolean hov = !sel && inside(mx, my, left + 1, ty, left + TAB_W, ty + TAB_H);
			if (sel) {
				g.fill(left + 1, ty, left + TAB_W + 1, ty + TAB_H, QuillPalette.PAPER);
				g.hLine(left + 1, left + TAB_W, ty, QuillPalette.RULE);
				g.hLine(left + 1, left + TAB_W, ty + TAB_H - 1, QuillPalette.RULE);
				g.fill(left + 1, ty, left + 3, ty + TAB_H, QuillPalette.OXBLOOD);
			} else if (hov) {
				g.fill(left + 1, ty, left + TAB_W, ty + TAB_H, QuillPalette.PAPER_HOVER);
			}
			g.drawString(font, tabNames.get(i), left + 9, ty + 5, sel ? QuillPalette.INK : QuillPalette.INK_SOFT, false);
		}

		// Rows
		List<QuillOption> rows = tabs.get(tab);
		QuillOption hovered = null;
		g.enableScissor(listX, listY, listR, listB);
		for (int i = 0; i < rows.size(); i++) {
			int ry = listY + 4 + i * ROW_H - (int) scroll;
			if (ry + ROW_H < listY || ry > listB) {
				continue;
			}
			QuillOption opt = rows.get(i);
			boolean hov = !showSupportCard && inside(mx, my, listX, Math.max(ry, listY), listR, Math.min(ry + ROW_H, listB));
			if (hov && !(opt instanceof QuillOption.Note n && n.heading)) {
				hovered = opt;
				g.fill(listX, ry, listR - 4, ry + ROW_H, QuillPalette.PAPER_HOVER);
			}
			renderRow(g, opt, ry, mx, my, hov);
			g.hLine(listX + 6, listR - 10, ry + ROW_H - 1, QuillPalette.RULE_FAINT);
		}
		g.disableScissor();
		int content = contentHeight();
		int view = listB - listY;
		if (content > view) {
			int barH = Math.max(12, view * view / content);
			int barY = listY + (int) ((view - barH) * (scroll / (content - view)));
			g.fill(listR - 3, barY, listR - 1, barY + barH, QuillPalette.INK_FAINT);
		}
		if (hovered != null) {
			described = hovered;
		}

		// Description panel
		g.hLine(listX, listR, listB, QuillPalette.RULE);
		renderDescription(g);
		g.hLine(left, right - 1, bottom - FOOTER_H, QuillPalette.INK);

		for (FooterButton b : footer) {
			b.render(g, mx, my, true);
		}
		if (showSupportCard) {
			renderSupportCard(g, mx, my);
		}
	}

	private void renderRow(GuiGraphics g, QuillOption opt, int ry, int mx, int my, boolean hov) {
		if (opt instanceof QuillOption.Note n) {
			if (n.heading) {
				smallCaps(g, n.label, listX + 10, ry + 8, QuillPalette.OXBLOOD);
				int lx = listX + 14 + smallCapsWidth(n.label);
				g.hLine(lx, listR - 10, ry + 12, QuillPalette.RULE);
			} else {
				g.drawString(font, n.label, listX + 10, ry + 6, QuillPalette.INK_SOFT, false);
				String t = font.plainSubstrByWidth(n.text.get(), listR - listX - 30 - font.width(n.label));
				g.drawString(font, t, listR - 10 - font.width(t), ry + 6, QuillPalette.INK_SOFT, false);
			}
			return;
		}
		boolean active = isActive(opt);
		boolean pending = opt.id != null && staged.isPending(opt.id);
		if (pending) {
			g.fill(listX + 4, ry + 5, listX + 6, ry + ROW_H - 6, QuillPalette.OXBLOOD);
		}
		int labelColor = !active ? QuillPalette.INK_FAINT : QuillPalette.INK;
		g.drawString(font, opt.label, listX + 10, ry + 6, labelColor, false);

		int cx = listR - 10 - CTRL_W, cy = ry + 3, ch = ROW_H - 6;
		boolean ctrlHov = hov && active && inside(mx, my, cx, cy, cx + CTRL_W, cy + ch);
		int ink = active ? QuillPalette.INK : QuillPalette.INK_FAINT;
		if (opt instanceof QuillOption.Toggle) {
			boolean on = Boolean.TRUE.equals(staged.get(opt.id));
			int tw = 44, tx = cx + CTRL_W - tw;
			g.fill(tx, cy, tx + tw, cy + ch, on && active ? QuillPalette.INK : QuillPalette.PAPER_DEEP);
			g.renderOutline(tx, cy, tw, ch, ctrlHov ? QuillPalette.OXBLOOD : QuillPalette.RULE);
			String s = on ? "On" : "Off";
			g.drawString(font, s, tx + (tw - font.width(s)) / 2, cy + 3, on && active ? QuillPalette.PAPER : QuillPalette.INK_SOFT, false);
		} else if (opt instanceof QuillOption.Cycle<?> c) {
			g.fill(cx, cy, cx + CTRL_W, cy + ch, ctrlHov ? QuillPalette.PAPER : QuillPalette.PAPER_DEEP);
			g.renderOutline(cx, cy, CTRL_W, ch, ctrlHov ? QuillPalette.OXBLOOD : QuillPalette.RULE);
			String s = font.plainSubstrByWidth(c.text(staged.get(opt.id)), CTRL_W - 20);
			g.drawString(font, "‹", cx + 4, cy + 3, QuillPalette.INK_SOFT, false);
			g.drawString(font, "›", cx + CTRL_W - 7, cy + 3, QuillPalette.INK_SOFT, false);
			g.drawString(font, s, cx + (CTRL_W - font.width(s)) / 2, cy + 3, ink, false);
		} else if (opt instanceof QuillOption.Slider sl) {
			int v = staged.<Integer>get(opt.id);
			int fillW = (int) Math.round((CTRL_W - 2) * sl.fraction(v));
			g.fill(cx, cy, cx + CTRL_W, cy + ch, QuillPalette.PAPER_DEEP);
			g.fill(cx + 1, cy + 1, cx + 1 + fillW, cy + ch - 1, active ? QuillPalette.OXBLOOD_WASH : QuillPalette.RULE_FAINT);
			g.fill(cx + fillW, cy, cx + fillW + 2, cy + ch, ink);
			g.renderOutline(cx, cy, CTRL_W, ch, ctrlHov || dragging == sl ? QuillPalette.OXBLOOD : QuillPalette.RULE);
			// Value sits left of the track, right-aligned in a fixed column, so the handle never covers it.
			String s = font.plainSubstrByWidth(sl.format.apply(v), SLIDER_VALUE_W);
			g.drawString(font, s, cx - 6 - font.width(s), cy + 3, ink, false);
		} else if (opt instanceof QuillOption.Action a) {
			g.fill(cx, cy, cx + CTRL_W, cy + ch, ctrlHov ? QuillPalette.INK : QuillPalette.PAPER_DEEP);
			g.renderOutline(cx, cy, CTRL_W, ch, QuillPalette.RULE);
			String s = a.button + " ›";
			g.drawString(font, s, cx + (CTRL_W - font.width(s)) / 2, cy + 3, ctrlHov ? QuillPalette.PAPER : QuillPalette.INK, false);
		}
	}

	private void renderDescription(GuiGraphics g) {
		int x = listX + 10, y = listB + 6, w = listR - listX - 20;
		if (described == null || described.description.isEmpty()) {
			g.drawString(font, "Hover an option to read what it does.", x, y, QuillPalette.INK_FAINT, false);
			return;
		}
		smallCaps(g, described.label.trim(), x, y, QuillPalette.INK);
		if (!(described instanceof QuillOption.Note) && !(described instanceof QuillOption.Action)) {
			String imp = described.impact.label;
			g.drawString(font, imp, listR - 10 - font.width(imp), y, described.impact.color, false);
		}
		List<FormattedCharSequence> lines = font.split(Component.literal(described.description), w);
		for (int i = 0; i < Math.min(3, lines.size()); i++) {
			g.drawString(font, lines.get(i), x, y + 11 + i * 10, QuillPalette.INK_SOFT, false);
		}
	}

	private int cardW() {
		return Math.min(260, listR - listX - 20);
	}

	private int cardH() {
		return 78;
	}

	private int cardY() {
		return Math.max(listY + 6, (listY + listB - cardH()) / 2);
	}

	private void renderSupportCard(GuiGraphics g, int mx, int my) {
		int w = cardW(), h = cardH(), x = (listX + listR - w) / 2, y = cardY();
		g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x402B2118);
		g.fill(x, y, x + w, y + h, QuillPalette.PAPER);
		g.renderOutline(x, y, w, h, QuillPalette.INK);
		g.renderOutline(x + 2, y + 2, w - 4, h - 4, QuillPalette.RULE);
		cup(g, x + 8, y + 8, QuillPalette.OXBLOOD);
		smallCaps(g, "A small note", x + 20, y + 8, QuillPalette.OXBLOOD);
		List<FormattedCharSequence> lines = font.split(Component.literal(
				"QuillCore is made in spare hours. If it bought you some frames, a coffee helps keep it going."), w - 16);
		for (int i = 0; i < Math.min(3, lines.size()); i++) {
			g.drawString(font, lines.get(i), x + 8, y + 21 + i * 10, QuillPalette.INK_SOFT, false);
		}
		for (FooterButton b : cardButtons) {
			b.render(g, mx, my, true);
		}
	}

	/** A tiny hand-drawn cup: body, handle, and a wisp of steam. */
	private static void cup(GuiGraphics g, int x, int y, int color) {
		g.renderOutline(x, y + 3, 7, 6, color);
		g.fill(x + 1, y + 6, x + 6, y + 8, color);
		g.renderOutline(x + 7, y + 4, 2, 3, color);
		g.vLine(x + 2, y - 1, y + 1, color);
		g.vLine(x + 4, y - 2, y, color);
	}

	/** Uppercase with shrunken lowercase letters and 1px tracking, reads like small caps. */
	private void smallCaps(GuiGraphics g, String s, int x, int y, int color) {
		float cx = x;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			String up = String.valueOf(Character.toUpperCase(ch));
			if (Character.isLowerCase(ch)) {
				g.pose().pushMatrix();
				g.pose().translate(cx, y + 1.5f);
				g.pose().scale(0.78f, 0.78f);
				g.drawString(font, up, 0, 0, color, false);
				g.pose().popMatrix();
				cx += font.width(up) * 0.78f + 1;
			} else {
				g.pose().pushMatrix();
				g.pose().translate(cx, y);
				g.drawString(font, up, 0, 0, color, false);
				g.pose().popMatrix();
				cx += font.width(up) + 1;
			}
		}
	}

	private int smallCapsWidth(String s) {
		float w = 0;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			int cw = font.width(String.valueOf(Character.toUpperCase(ch)));
			w += (Character.isLowerCase(ch) ? cw * 0.78f : cw) + 1;
		}
		return Math.round(w);
	}

	// input

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (showSupportCard) {
			for (FooterButton b : cardButtons) {
				if (b.click(mx, my)) {
					return true;
				}
			}
			int w = cardW(), x = (listX + listR - w) / 2, y = cardY();
			if (inside(mx, my, x, y, x + w, y + cardH())) {
				return true;
			}
		}
		for (FooterButton b : footer) {
			if (b.click(mx, my)) {
				return true;
			}
		}
		for (int i = 0; i < tabNames.size(); i++) {
			int ty = top + HEADER_H + 4 + i * TAB_H;
			if (inside(mx, my, left + 1, ty, left + TAB_W, ty + TAB_H)) {
				if (tab != i) {
					tab = i;
					scroll = 0;
					described = null;
				}
				return true;
			}
		}
		if (showSupportCard || !inside(mx, my, listX, listY, listR, listB)) {
			return super.mouseClicked(mx, my, button);
		}
		QuillOption opt = rowAt(my);
		if (opt == null || !isActive(opt)) {
			return true;
		}
		int cx = listR - 10 - CTRL_W;
		if (opt instanceof QuillOption.Toggle) {
			staged.stage(opt.id, !Boolean.TRUE.equals(staged.get(opt.id)));
		} else if (opt instanceof QuillOption.Cycle<?> c) {
			staged.stage(opt.id, c.step(staged.get(opt.id), button == 1 ? -1 : 1));
		} else if (opt instanceof QuillOption.Slider sl && mx >= cx) {
			dragging = sl;
			dragTo(mx);
		} else if (opt instanceof QuillOption.Action a && mx >= cx) {
			a.run.run();
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (dragging != null) {
			dragTo(mx);
			return true;
		}
		return super.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		dragging = null;
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		if (inside(mx, my, listX, listY, listR, listB)) {
			scroll -= v * ROW_H;
			clampScroll();
			return true;
		}
		return super.mouseScrolled(mx, my, h, v);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			applyPending();
			return true;
		}
		return super.keyPressed(key, scan, mods);
	}

	private void dragTo(double mx) {
		int cx = listR - 10 - CTRL_W;
		staged.stage(dragging.id, dragging.snap((mx - cx) / (double) CTRL_W));
	}

	private QuillOption rowAt(double my) {
		int i = (int) Math.floor((my - listY - 4 + scroll) / ROW_H);
		List<QuillOption> rows = tabs.get(tab);
		return i >= 0 && i < rows.size() ? rows.get(i) : null;
	}

	private boolean isActive(QuillOption opt) {
		if (opt instanceof QuillOption.Note) {
			return false;
		}
		return opt.requires == null || Boolean.TRUE.equals(staged.get(opt.requires));
	}

	private int contentHeight() {
		return tabs.isEmpty() ? 0 : tabs.get(tab).size() * ROW_H + 8;
	}

	private void clampScroll() {
		scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight() - (listB - listY))));
	}

	private static boolean inside(double mx, double my, int x0, int y0, int x1, int y1) {
		return mx >= x0 && mx < x1 && my >= y0 && my < y1;
	}

	private final class FooterButton {
		final int x, y, w, h;
		final String label;
		final boolean coffee;
		final Supplier<Boolean> enabled;
		final Runnable action;

		FooterButton(int x, int y, int w, int h, String label, boolean coffee, Supplier<Boolean> enabled, Runnable action) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.label = label;
			this.coffee = coffee;
			this.enabled = enabled;
			this.action = action;
		}

		void render(GuiGraphics g, int mx, int my, boolean interactive) {
			boolean on = enabled.get();
			boolean hov = interactive && on && inside(mx, my, x, y, x + w, y + h);
			g.fill(x, y, x + w, y + h, hov ? QuillPalette.INK : QuillPalette.PAPER);
			g.renderOutline(x, y, w, h, on ? QuillPalette.INK : QuillPalette.INK_FAINT);
			int text = hov ? QuillPalette.PAPER : on ? QuillPalette.INK : QuillPalette.INK_FAINT;
			if (coffee) {
				cup(g, x + 5, y + 4, hov ? QuillPalette.PAPER : QuillPalette.OXBLOOD);
				g.drawString(font, label, x + 18, y + 4, text, false);
			} else {
				g.drawString(font, label, x + (w - font.width(label)) / 2, y + 4, text, false);
			}
		}

		boolean click(double mx, double my) {
			if (enabled.get() && inside(mx, my, x, y, x + w, y + h)) {
				action.run();
				return true;
			}
			return false;
		}
	}
}
