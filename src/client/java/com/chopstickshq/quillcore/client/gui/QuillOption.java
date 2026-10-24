package com.chopstickshq.quillcore.client.gui;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

/** One row in the settings list. Values live in {@code StagedSettings} under {@link #id}. */
abstract sealed class QuillOption {
	/** How much a row costs (or saves), drives the label under the description. */
	enum Impact {
		NONE("No performance impact", QuillPalette.INK_SOFT),
		LOW("Low performance impact", QuillPalette.INK_SOFT),
		MEDIUM("Medium performance impact", QuillPalette.OCHRE),
		HIGH("High performance impact", QuillPalette.OXBLOOD),
		SAVES("Improves performance", QuillPalette.MOSS);

		final String label;
		final int color;

		Impact(String label, int color) {
			this.label = label;
			this.color = color;
		}
	}

	final String id;
	final String label;
	final String description;
	final Impact impact;
	/** Row is greyed out unless this staged boolean is true (null = always active). */
	String requires;

	QuillOption(String id, String label, String description, Impact impact) {
		this.id = id;
		this.label = label;
		this.description = description;
		this.impact = impact;
	}

	QuillOption requires(String toggleId) {
		this.requires = toggleId;
		return this;
	}

	static final class Toggle extends QuillOption {
		Toggle(String id, String label, String description, Impact impact) {
			super(id, label, description, impact);
		}
	}

	static final class Cycle<T> extends QuillOption {
		final List<T> values;
		final Function<T, String> format;

		Cycle(String id, String label, String description, Impact impact, List<T> values, Function<T, String> format) {
			super(id, label, description, impact);
			this.values = values;
			this.format = format;
		}

		@SuppressWarnings("unchecked")
		Object step(Object current, int dir) {
			int i = values.indexOf((T) current);
			int n = values.size();
			return values.get(((i < 0 ? 0 : i + dir) % n + n) % n);
		}

		@SuppressWarnings("unchecked")
		String text(Object current) {
			return format.apply((T) current);
		}
	}

	static final class Slider extends QuillOption {
		final int min;
		final int max;
		final int step;
		final IntFunction<String> format;

		Slider(String id, String label, String description, Impact impact, int min, int max, int step, IntFunction<String> format) {
			super(id, label, description, impact);
			this.min = min;
			this.max = Math.max(min, max);
			this.step = Math.max(1, step);
			this.format = format;
		}

		int snap(double t) {
			double clamped = Math.max(0.0, Math.min(1.0, t));
			int v = min + (int) Math.round(clamped * (max - min) / step) * step;
			return Math.max(min, Math.min(max, v));
		}

		double fraction(int v) {
			return max == min ? 0.0 : (double) (v - min) / (max - min);
		}
	}

	static final class Action extends QuillOption {
		final String button;
		final Runnable run;

		Action(String label, String description, String button, Runnable run) {
			super(null, label, description, Impact.NONE);
			this.button = button;
			this.run = run;
		}
	}

	/** Static text: a section rule ({@code heading}) or an info line. */
	static final class Note extends QuillOption {
		final boolean heading;
		final java.util.function.Supplier<String> text;

		Note(String label, boolean heading, java.util.function.Supplier<String> text, String description) {
			super(null, label, description, Impact.NONE);
			this.heading = heading;
			this.text = text;
		}
	}
}
