package dev.notalpha.dashloader.client.model;

import dev.notalpha.dashloader.DashLoader;
import dev.notalpha.dashloader.api.CachingData;
import dev.notalpha.dashloader.api.DashModule;
import dev.notalpha.dashloader.api.cache.Cache;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.api.registry.RegistryReader;
import dev.notalpha.dashloader.api.registry.RegistryWriter;
import dev.notalpha.dashloader.client.model.geometry.CachedGeometry;
import dev.notalpha.dashloader.config.ConfigHandler;
import dev.notalpha.dashloader.config.Option;
import dev.notalpha.taski.builtin.StepTask;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Caches the baked geometry (the quads) of models, keyed by what influences the bake.
 * <br><br>
 * Only the final quads are cached; everything built around them (variants, multipart, items, whatever other mods add) is
 * created by the game as usual. A model that is not in the cache is simply baked normally, so unknown or modified
 * models can never break the cache, they only miss out on the speedup.
 */
public class ModelModule implements DashModule<ModelModule.Data> {
	/** Filled while the game bakes, written to the cache at the end. */
	public static final CachingData<ConcurrentHashMap<String, Baked>> SAVE_GEOMETRY = new CachingData<>(CacheStatus.SAVE);
	/** Keys which were baked to different results, these cannot be trusted and are not cached. */
	public static final CachingData<Set<String>> SAVE_CONFLICTS = new CachingData<>(CacheStatus.SAVE);
	/** Read by the bake while loading. */
	public static final CachingData<GeometryCache> LOAD_GEOMETRY = new CachingData<>(CacheStatus.LOAD);

	public static final AtomicInteger HITS = new AtomicInteger();
	public static final AtomicInteger MISSES = new AtomicInteger();

	public static void record(String key, @Nullable Object content, QuadCollection result) {
		var map = SAVE_GEOMETRY.get(CacheStatus.SAVE);
		var conflicts = SAVE_CONFLICTS.get(CacheStatus.SAVE);
		if (map == null || conflicts == null) {
			return;
		}

		Baked previous = map.putIfAbsent(key, new Baked(content, result));
		if (previous != null && (!Objects.equals(previous.content, content) || !sameQuads(previous.result, result))) {
			conflicts.add(key);
		}
	}

	public static void logStats() {
		int hits = HITS.get();
		int misses = MISSES.get();
		if (hits + misses > 0) {
			DashLoader.LOG.info("Model geometry cache: {} baked from cache, {} baked normally", hits, misses);
		}
	}

	@Override
	public void reset(Cache cache) {
		HITS.set(0);
		MISSES.set(0);
		SAVE_GEOMETRY.reset(cache, new ConcurrentHashMap<>());
		SAVE_CONFLICTS.reset(cache, ConcurrentHashMap.newKeySet());
		LOAD_GEOMETRY.reset(cache, new GeometryCache(new Identifier[0], new HashMap<>()));
	}

	@Override
	public Data save(RegistryWriter writer, StepTask task) {
		var baked = SAVE_GEOMETRY.get(CacheStatus.SAVE);
		var conflicts = SAVE_CONFLICTS.get(CacheStatus.SAVE);
		if (baked == null || conflicts == null) {
			return null;
		}

		task.reset(baked.size());
		List<Identifier> spriteTable = new ArrayList<>();
		Map<Identifier, Integer> spriteIndex = new HashMap<>();
		Map<Identifier, Material.Baked> liveSprites = new HashMap<>();
		List<String> keys = new ArrayList<>();
		List<CachedGeometry> geometries = new ArrayList<>();
		List<QuadCollection> originals = new ArrayList<>();

		int conflicting = 0;
		for (Map.Entry<String, Baked> entry : baked.entrySet()) {
			task.next();
			if (conflicts.contains(entry.getKey())) {
				conflicting++;
				continue;
			}

			QuadCollection result = entry.getValue().result;
			for (BakedQuad quad : result.getAll()) {
				var sprite = quad.materialInfo().sprite();
				liveSprites.putIfAbsent(sprite.contents().name(), new Material.Baked(sprite, false));
			}
			keys.add(entry.getKey());
			geometries.add(CachedGeometry.of(result, id -> spriteIndex.computeIfAbsent(id, i -> {
				spriteTable.add(i);
				return spriteTable.size() - 1;
			})));
			originals.add(result);
		}

		// Prove that every entry turns back into exactly the quads it was made from, entries which don't are left out.
		Identifier[] table = spriteTable.toArray(Identifier[]::new);
		List<String> outKeys = new ArrayList<>(keys.size());
		List<CachedGeometry> outGeometries = new ArrayList<>(keys.size());
		int failed = 0;
		for (int i = 0; i < keys.size(); i++) {
			boolean same;
			try {
				same = sameQuads(originals.get(i), geometries.get(i).build(table, liveSprites::get, null));
			} catch (RuntimeException e) {
				same = false;
			}
			if (same) {
				outKeys.add(keys.get(i));
				outGeometries.add(geometries.get(i));
			} else {
				failed++;
			}
		}

		DashLoader.LOG.info("Saved {} model geometries ({} conflicting and {} failing verification were left out)", outKeys.size(), conflicting, failed);
		List<String> spriteNames = new ArrayList<>(table.length);
		for (Identifier identifier : table) {
			spriteNames.add(identifier.toString());
		}
		return new Data(spriteNames, outKeys, outGeometries);
	}

	@Override
	public void load(Data data, RegistryReader reader, StepTask task) {
		Identifier[] sprites = data.sprites.stream().map(Identifier::parse).toArray(Identifier[]::new);
		Map<String, CachedGeometry> map = new HashMap<>(data.keys.size() * 2);
		for (int i = 0; i < data.keys.size(); i++) {
			map.put(data.keys.get(i), data.geometries.get(i));
		}
		LOAD_GEOMETRY.set(CacheStatus.LOAD, new GeometryCache(sprites, map));
	}

	@Override
	public Class<Data> getDataClass() {
		return Data.class;
	}

	@Override
	public boolean isActive() {
		return ConfigHandler.optionActive(Option.CACHE_MODEL_LOADER);
	}

	@Override
	public float taskWeight() {
		return 5;
	}

	private static boolean sameQuads(QuadCollection a, QuadCollection b) {
		if (!a.unculled.equals(b.unculled) || !a.getAll().equals(b.getAll())) {
			return false;
		}
		for (Direction direction : Direction.values()) {
			if (!a.getQuads(direction).equals(b.getQuads(direction))) {
				return false;
			}
		}
		return true;
	}

	public record Baked(@Nullable Object content, QuadCollection result) {
	}

	public record GeometryCache(Identifier[] sprites, Map<String, CachedGeometry> geometries) {
	}

	public static final class Data {
		public final List<String> sprites;
		public final List<String> keys;
		public final List<CachedGeometry> geometries;

		public Data(List<String> sprites, List<String> keys, List<CachedGeometry> geometries) {
			this.sprites = sprites;
			this.keys = keys;
			this.geometries = geometries;
		}
	}
}
