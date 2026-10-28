package dev.notalpha.dashloader.client.sprite.content;

import com.mojang.blaze3d.platform.NativeImage;
import dev.notalpha.dashloader.DashLoader;
import dev.notalpha.dashloader.api.CachingData;
import dev.notalpha.dashloader.api.DashModule;
import dev.notalpha.dashloader.api.cache.Cache;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.api.registry.RegistryReader;
import dev.notalpha.dashloader.api.registry.RegistryWriter;
import dev.notalpha.dashloader.config.ConfigHandler;
import dev.notalpha.dashloader.config.Option;
import dev.notalpha.taski.builtin.StepTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generating the mip levels of a sprite only depends on its image and a few settings, but it takes a good amount of
 * time for all the sprites. This saves the images of every level.
 * <br><br>
 * The first level is the image of the sprite itself, which the generation also modifies (it fills in the transparent
 * pixels). It is saved after that modification and written back into the image on load so the result is the same no matter
 * where the image came from.
 */
public class MipmapModule implements DashModule<MipmapModule.Data> {
	/** Filled while the game generates mip levels, the full chain of images including the first level. */
	public static final CachingData<ConcurrentHashMap<String, NativeImage[]>> SAVE_LEVELS = new CachingData<>(CacheStatus.SAVE);
	/** Entries are taken out when they are used, an image can only belong to one sprite. */
	public static final CachingData<ConcurrentHashMap<String, NativeImage[]>> LOAD_LEVELS = new CachingData<>(CacheStatus.LOAD);

	/**
	 * Images which were read from the cache. They were saved after the sprite loading had finished with them, so what the
	 * mip generation does to a plain image (filling in transparent pixels) has already been done to them.
	 */
	public static final java.util.Set<NativeImage> FINISHED_IMAGES = ConcurrentHashMap.newKeySet();

	public static final java.util.concurrent.atomic.AtomicInteger HITS = new java.util.concurrent.atomic.AtomicInteger();
	public static final java.util.concurrent.atomic.AtomicInteger MISSES = new java.util.concurrent.atomic.AtomicInteger();
	public static final java.util.concurrent.atomic.AtomicInteger SKIPPED = new java.util.concurrent.atomic.AtomicInteger();

	public static void logStats() {
		if (HITS.get() + MISSES.get() + SKIPPED.get() > 0) {
			DashLoader.LOG.info("Mip levels: {} from cache, {} generated, {} already finished", HITS.get(), MISSES.get(), SKIPPED.get());
		}
	}

	@Override
	public void reset(Cache cache) {
		HITS.set(0);
		MISSES.set(0);
		SKIPPED.set(0);
		FINISHED_IMAGES.clear();
		SAVE_LEVELS.reset(cache, new ConcurrentHashMap<>());
		LOAD_LEVELS.reset(cache, new ConcurrentHashMap<>());
	}

	@Override
	public Data save(RegistryWriter writer, StepTask task) {
		var levels = SAVE_LEVELS.get(CacheStatus.SAVE);
		if (levels == null) {
			return null;
		}

		task.reset(levels.size());
		List<String> keys = new ArrayList<>(levels.size());
		List<Integer> counts = new ArrayList<>(levels.size());
		List<Integer> ids = new ArrayList<>();
		int skipped = 0;
		for (Map.Entry<String, NativeImage[]> entry : levels.entrySet()) {
			task.next();
			NativeImage[] images = entry.getValue();
			// Sprites which did not end up in an atlas get closed, there is nothing left to read from their images.
			boolean usable = true;
			for (NativeImage image : images) {
				usable &= image != null && !image.isClosed();
			}
			if (!usable) {
				skipped++;
				continue;
			}

			// Registry ids are only committed once every image of the entry is in.
			List<Integer> entryIds = new ArrayList<>(images.length);
			try {
				for (NativeImage image : images) {
					entryIds.add(writer.add(image));
				}
			} catch (RuntimeException e) {
				skipped++;
				continue;
			}
			keys.add(entry.getKey());
			counts.add(images.length);
			ids.addAll(entryIds);
		}
		DashLoader.LOG.info("Saved the mip levels of {} sprites ({} left out)", keys.size(), skipped);
		return new Data(keys, counts.stream().mapToInt(Integer::intValue).toArray(), ids.stream().mapToInt(Integer::intValue).toArray());
	}

	@Override
	public void load(Data data, RegistryReader reader, StepTask task) {
		var out = new ConcurrentHashMap<String, NativeImage[]>(data.keys.size() * 2);
		int offset = 0;
		for (int i = 0; i < data.keys.size(); i++) {
			NativeImage[] images = new NativeImage[data.counts[i]];
			for (int j = 0; j < images.length; j++) {
				images[j] = reader.get(data.ids[offset++]);
			}
			out.put(data.keys.get(i), images);
		}
		LOAD_LEVELS.set(CacheStatus.LOAD, out);
	}

	@Override
	public Class<Data> getDataClass() {
		return Data.class;
	}

	@Override
	public boolean isActive() {
		return ConfigHandler.optionActive(Option.CACHE_SPRITE_CONTENT);
	}

	@Override
	public float taskWeight() {
		return 3;
	}

	public static final class Data {
		public final List<String> keys;
		/** How many images each key has. */
		public final int[] counts;
		/** Registry ids of the images, the images of all keys following each other. */
		public final int[] ids;

		public Data(List<String> keys, int[] counts, int[] ids) {
			this.keys = keys;
			this.counts = counts;
			this.ids = ids;
		}
	}
}
