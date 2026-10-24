package com.chopstickshq.quillcore.client.gui;

/** Paper-and-ink colours (ARGB). Flat fills only. */
final class QuillPalette {
	static final int SHADE = 0xB01A140E;
	static final int PAPER = 0xFFF2E8D2;
	static final int PAPER_DEEP = 0xFFE5D6B4;
	static final int PAPER_HOVER = 0xFFEADDC0;
	static final int RULE_FAINT = 0x1A2B2118;
	static final int RULE = 0xFF8C7759;
	static final int INK = 0xFF2B2118;
	static final int INK_SOFT = 0xFF77654C;
	static final int INK_FAINT = 0xFFA89676;
	static final int OXBLOOD = 0xFF9A3B28;
	static final int OXBLOOD_WASH = 0x339A3B28;
	static final int OCHRE = 0xFFA06E16;
	static final int MOSS = 0xFF4E6A2C;
	static final int DROP_SHADOW = 0x402B2118;
	static final int VEIL = 0x66F2E8D2;

	private QuillPalette() {}

	static int mix(int from, int to, float t) {
		if (t <= 0f) {
			return from;
		}
		if (t >= 1f) {
			return to;
		}
		int a = channel(from, to, 24, t);
		int r = channel(from, to, 16, t);
		int g = channel(from, to, 8, t);
		int b = channel(from, to, 0, t);
		return a << 24 | r << 16 | g << 8 | b;
	}

	private static int channel(int from, int to, int shift, float t) {
		int x = from >>> shift & 0xFF;
		int y = to >>> shift & 0xFF;
		return Math.round(x + (y - x) * t);
	}
}
