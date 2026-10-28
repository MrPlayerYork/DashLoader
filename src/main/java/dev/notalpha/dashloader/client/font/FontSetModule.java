package dev.notalpha.dashloader.client.font;

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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import com.mojang.blaze3d.font.GlyphProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When a font is applied the game goes through every glyph of every provider to find out which providers are used and to
 * group the glyphs by width (for obfuscated text). That is a single threaded step in the middle of the loading, and its result only
 * depends on the providers. This module saves the result per font.
 */
public class FontSetModule implements DashModule<FontSetModule.Data> {
	/** Name of the font that is currently being created, set by the font manager. */
	public static final ThreadLocal<String> CURRENT_FONT = new ThreadLocal<>();
	public static final CachingData<ConcurrentHashMap<String, Entry>> SAVE_ENTRIES = new CachingData<>(CacheStatus.SAVE);
	public static final CachingData<Map<String, Entry>> LOAD_ENTRIES = new CachingData<>(CacheStatus.LOAD);

	@Override
	public void reset(Cache cache) {
		SAVE_ENTRIES.reset(cache, new ConcurrentHashMap<>());
		LOAD_ENTRIES.reset(cache, new HashMap<>());
	}

	@Override
	public Data save(RegistryWriter writer, StepTask task) {
		var entries = SAVE_ENTRIES.get(CacheStatus.SAVE);
		if (entries == null) {
			return null;
		}
		DashLoader.LOG.info("Saved {} font sets", entries.size());
		return new Data(new ArrayList<>(entries.keySet()), new ArrayList<>(entries.values()));
	}

	@Override
	public void load(Data data, RegistryReader reader, StepTask task) {
		Map<String, Entry> map = new HashMap<>();
		for (int i = 0; i < data.keys.size(); i++) {
			map.put(data.keys.get(i), data.entries.get(i));
		}
		LOAD_ENTRIES.set(CacheStatus.LOAD, map);
	}

	@Override
	public Class<Data> getDataClass() {
		return Data.class;
	}

	@Override
	public boolean isActive() {
		return ConfigHandler.optionActive(Option.CACHE_FONT);
	}

	public static final class Data {
		public final List<String> keys;
		public final List<Entry> entries;

		public Data(List<String> keys, List<Entry> entries) {
			this.keys = keys;
			this.entries = entries;
		}
	}

	/**
	 * The outcome of selecting the providers of a font.
	 */
	public static final class Entry {
		/** How many providers the font had, to notice when it is not the same font any more. */
		public final int providerCount;
		/** The used providers as indices in the list of all providers. */
		public final int[] used;
		/** The widths which have glyphs, and how many glyphs each has. The code points of all widths follow each other in {@link #codepoints}. */
		public final int[] widths;
		public final int[] lengths;
		public final int[] codepoints;

		public Entry(int providerCount, int[] used, int[] widths, int[] lengths, int[] codepoints) {
			this.providerCount = providerCount;
			this.used = used;
			this.widths = widths;
			this.lengths = lengths;
			this.codepoints = codepoints;
		}

		/** Captures what the game decided, or null if it cannot be represented. */
		public static Entry of(List<GlyphProvider.Conditional> all, List<GlyphProvider> result, Int2ObjectMap<IntList> glyphsByWidth) {
			Map<GlyphProvider, Integer> indices = new IdentityHashMap<>();
			for (int i = all.size() - 1; i >= 0; i--) {
				indices.put(all.get(i).provider(), i);
			}
			int[] used = new int[result.size()];
			for (int i = 0; i < used.length; i++) {
				Integer index = indices.get(result.get(i));
				if (index == null) {
					return null;
				}
				used[i] = index;
			}

			int[] widths = glyphsByWidth.keySet().toIntArray();
			java.util.Arrays.sort(widths);
			int[] lengths = new int[widths.length];
			int total = 0;
			for (int i = 0; i < widths.length; i++) {
				lengths[i] = glyphsByWidth.get(widths[i]).size();
				total += lengths[i];
			}
			int[] codepoints = new int[total];
			int offset = 0;
			for (int i = 0; i < widths.length; i++) {
				IntList list = glyphsByWidth.get(widths[i]);
				for (int j = 0; j < list.size(); j++) {
					codepoints[offset++] = list.getInt(j);
				}
			}
			return new Entry(all.size(), used, widths, lengths, codepoints);
		}

		/** Fills the width groups and returns the used providers. Null if the font does not match this entry. */
		public List<GlyphProvider> restore(List<GlyphProvider.Conditional> all, Int2ObjectMap<IntList> glyphsByWidth) {
			if (all.size() != this.providerCount) {
				return null;
			}
			List<GlyphProvider> out = new ArrayList<>(this.used.length);
			for (int index : this.used) {
				out.add(all.get(index).provider());
			}
			int offset = 0;
			for (int i = 0; i < this.widths.length; i++) {
				glyphsByWidth.put(this.widths[i], new IntArrayList(this.codepoints, offset, this.lengths[i]));
				offset += this.lengths[i];
			}
			return out;
		}
	}
}
