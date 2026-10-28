package dev.notalpha.dashloader.misc;

import org.jetbrains.annotations.Nullable;

/**
 * Lets the development tools in {@code src/dev} (state dumps, stage timings) observe what DashLoader caches. The release jar does
 * not contain any of those tools, so {@link #sink} stays null there and every call is a cheap no-op.
 */
public final class DebugHooks {
	/** Set by the development-only mod before the game loads, null in a release. */
	@Nullable
	public static volatile Sink sink;

	private DebugHooks() {
	}

	public static boolean enabled() {
		Sink sink = DebugHooks.sink;
		return sink != null && sink.enabled();
	}

	/**
	 * @param value Either a precomputed hash ({@link Long}) or something the sink knows how to hash (images, quads).
	 */
	public static void record(String category, String key, Object value) {
		Sink sink = DebugHooks.sink;
		if (sink != null) {
			sink.record(category, key, value);
		}
	}

	public static void write(String name) {
		Sink sink = DebugHooks.sink;
		if (sink != null) {
			sink.write(name);
		}
	}

	public interface Sink {
		boolean enabled();

		void record(String category, String key, Object value);

		void write(String name);
	}
}
