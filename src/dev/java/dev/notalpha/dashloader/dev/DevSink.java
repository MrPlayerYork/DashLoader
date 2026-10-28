package dev.notalpha.dashloader.dev;

import com.mojang.blaze3d.platform.NativeImage;
import dev.notalpha.dashloader.misc.DebugHooks;
import net.minecraft.client.resources.model.geometry.QuadCollection;

/** Feeds what DashLoader reports into {@link StateDump}. */
public final class DevSink implements DebugHooks.Sink {
	@Override
	public boolean enabled() {
		return StateDump.enabled();
	}

	@Override
	public void record(String category, String key, Object value) {
		long hash;
		if (value instanceof Long l) {
			hash = l;
		} else if (value instanceof NativeImage[] images) {
			hash = StateDump.hash(images);
		} else if (value instanceof QuadCollection quads) {
			hash = StateDump.hash(quads);
		} else {
			throw new IllegalArgumentException("Can't hash " + value);
		}
		StateDump.record(category, key, hash);
	}

	@Override
	public void write(String name) {
		StateDump.write(name);
	}
}
