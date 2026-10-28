package dev.notalpha.dashloader.client.model.geometry;

import dev.notalpha.dashloader.DashLoader;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.model.ModelModule;
import dev.notalpha.dashloader.misc.StateDump;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * The caching around a geometry bake, shared by every place that bakes geometry. On save the result is recorded, on load a
 * matching cached result replaces the bake. Anything which is not found in the cache is baked as the game normally would.
 */
public final class GeometryCaching {
	private static final AtomicBoolean WARNED = new AtomicBoolean();

	private GeometryCaching() {
	}

	/**
	 * @param content Something which is equal for equal input geometry, used to notice a name being shared by different geometry. May be null.
	 * @param original Does the normal bake.
	 */
	public static QuadCollection bake(@Nullable Object content, TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name, Supplier<QuadCollection> original) {
		QuadCollection out = bakeInner(content, slots, baker, state, name, original);
		if (StateDump.enabled()) {
			StateDump.record("geometry", GeometryKey.of(name.debugName(), slots, state), StateDump.hash(out));
		}
		return out;
	}

	private static QuadCollection bakeInner(@Nullable Object content, TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name, Supplier<QuadCollection> original) {
		var load = ModelModule.LOAD_GEOMETRY.get(CacheStatus.LOAD);
		boolean saving = ModelModule.SAVE_GEOMETRY.get(CacheStatus.SAVE) != null;
		if (load == null && !saving) {
			return original.get();
		}

		String key = GeometryKey.of(name.debugName(), slots, state);
		if (load != null) {
			CachedGeometry cached = load.geometries().get(key);
			if (cached != null) {
				try {
					QuadCollection result = cached.build(load.sprites(), sprite -> baker.materials().get(new Material(sprite), name), baker);
					ModelModule.HITS.incrementAndGet();
					return result;
				} catch (RuntimeException e) {
					// Never let a bad cache entry break the game, bake it normally instead.
					if (WARNED.compareAndSet(false, true)) {
						DashLoader.LOG.warn("Could not use a cached model geometry, baking it normally. This is only logged once.", e);
					}
				}
			}
			ModelModule.MISSES.incrementAndGet();
		}

		QuadCollection result = original.get();
		if (saving) {
			ModelModule.record(key, content, result);
		}
		return result;
	}
}
