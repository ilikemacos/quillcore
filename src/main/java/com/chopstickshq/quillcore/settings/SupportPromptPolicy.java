package com.chopstickshq.quillcore.settings;

/**
 * Decides when the "buy me a coffee" card may appear inside the QuillCore settings screen.
 * Pure: no Minecraft, no clock, callers pass {@code nowMs}. Never consulted outside the settings screen.
 *
 * <p>Rule: shown only when the screen has been opened at least {@link #MIN_OPENS} times since it was
 * last shown AND at least {@link #MIN_INTERVAL_MS} has passed since then (the very first time only
 * the open count matters). "Don't show again" is permanent.
 */
public final class SupportPromptPolicy {
	public static final int MIN_OPENS = 10;
	public static final long MIN_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000;

	private SupportPromptPolicy() {}

	/** Persisted counters (mirrors QuillConfig bmcOpenCount / bmcLastShownEpochMs / bmcNeverShow). */
	public record State(int openCount, long lastShownEpochMs, boolean neverShow) {
		public State {
			openCount = Math.max(0, openCount);
			lastShownEpochMs = Math.max(0L, lastShownEpochMs);
		}
	}

	/** Count one settings-screen open. Saturates instead of overflowing. */
	public static State recordOpen(State s) {
		int next = s.openCount() == Integer.MAX_VALUE ? Integer.MAX_VALUE : s.openCount() + 1;
		return new State(next, s.lastShownEpochMs(), s.neverShow());
	}

	public static boolean shouldShow(State s, long nowMs) {
		if (s.neverShow() || s.openCount() < MIN_OPENS) {
			return false;
		}
		if (s.lastShownEpochMs() == 0L) {
			return true;
		}
		long elapsed = nowMs - s.lastShownEpochMs();
		// Clock moved backwards: stay quiet rather than nag.
		return elapsed >= MIN_INTERVAL_MS;
	}

	/** The card was displayed: reset the open counter and remember when. */
	public static State recordShown(State s, long nowMs) {
		return new State(0, nowMs, s.neverShow());
	}

	public static State recordNeverShow(State s) {
		return new State(s.openCount(), s.lastShownEpochMs(), true);
	}
}
