package dev.notalpha.dashloader.dev;

import dev.notalpha.dashloader.DashLoader;
import dev.notalpha.dashloader.misc.ProfilerUtil;

import java.util.concurrent.CompletableFuture;

/**
 * Logs when (relative to the start of the reload) and for how long a stage of the Minecraft reload runs. Only used by the mixins in
 * {@code mixin.profile}, which are only applied when the DASHLOADER_PROFILE environment variable is set.
 */
public final class StageProfiler {
	private StageProfiler() {
	}

	public static boolean enabled() {
		return System.getenv("DASHLOADER_PROFILE") != null || Boolean.getBoolean("dashloader.profile");
	}

	public static long begin() {
		return System.currentTimeMillis();
	}

	public static <T> CompletableFuture<T> track(String stage, long startMillis, CompletableFuture<T> future) {
		return future.whenComplete((value, error) -> end(stage, startMillis));
	}

	public static void end(String stage, long startMillis) {
		long now = System.currentTimeMillis();
		long base = ProfilerUtil.RELOAD_START;
		DashLoader.LOG.info("[timeline] {} | start +{} ms | end +{} ms | took {} ms | thread {}", stage, startMillis - base, now - base, now - startMillis, Thread.currentThread().getName());
	}
}
