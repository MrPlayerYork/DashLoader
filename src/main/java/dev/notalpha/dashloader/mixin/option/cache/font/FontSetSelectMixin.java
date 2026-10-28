package dev.notalpha.dashloader.mixin.option.cache.font;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.font.GlyphProvider;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.font.FontSetModule;
import dev.notalpha.dashloader.misc.StateDump;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Replaces the scan over all the glyphs of a font with the outcome of an earlier scan.
 */
@Mixin(FontSet.class)
public abstract class FontSetSelectMixin {
	@Shadow
	@Final
	private Int2ObjectMap<IntList> glyphsByWidth;

	@WrapMethod(method = "selectProviders(Ljava/util/List;Ljava/util/Set;)Ljava/util/List;")
	private List<GlyphProvider> dashloaderSelectProviders(List<GlyphProvider.Conditional> all, Set<FontOption> options, Operation<List<GlyphProvider>> original) {
		List<GlyphProvider> result = this.dashloaderSelect(all, options, original);
		String font = FontSetModule.CURRENT_FONT.get();
		if (StateDump.enabled() && font != null) {
			long h = 17;
			for (GlyphProvider provider : result) {
				for (int i = 0; i < all.size(); i++) {
					if (all.get(i).provider() == provider) {
						h = h * 31 + i;
					}
				}
			}
			var widths = new java.util.TreeMap<Integer, IntList>(this.glyphsByWidth);
			for (var e : widths.entrySet()) {
				h = h * 31 + e.getKey();
				h = h * 31 + e.getValue().hashCode();
			}
			StateDump.record("font", font + "|" + options, h);
		}
		return result;
	}

	private List<GlyphProvider> dashloaderSelect(List<GlyphProvider.Conditional> all, Set<FontOption> options, Operation<List<GlyphProvider>> original) {
		String font = FontSetModule.CURRENT_FONT.get();
		var load = FontSetModule.LOAD_ENTRIES.get(CacheStatus.LOAD);
		var save = FontSetModule.SAVE_ENTRIES.get(CacheStatus.SAVE);
		if (font == null || (load == null && save == null)) {
			return original.call(all, options);
		}

		String key = font + "|" + options.stream().map(Objects::toString).sorted().collect(Collectors.joining(","));
		if (load != null) {
			FontSetModule.Entry entry = load.get(key);
			if (entry != null) {
				List<GlyphProvider> restored = entry.restore(all, this.glyphsByWidth);
				if (restored != null) {
					return restored;
				}
			}
		}

		List<GlyphProvider> result = original.call(all, options);
		if (save != null) {
			FontSetModule.Entry entry = FontSetModule.Entry.of(all, result, this.glyphsByWidth);
			if (entry != null && matches(entry, all, result)) {
				save.put(key, entry);
			}
		}
		return result;
	}

	/** Only entries which turn back into exactly what the game decided are kept. */
	private boolean matches(FontSetModule.Entry entry, List<GlyphProvider.Conditional> all, List<GlyphProvider> result) {
		Int2ObjectMap<IntList> restoredWidths = new Int2ObjectOpenHashMap<>();
		List<GlyphProvider> restored = entry.restore(all, restoredWidths);
		if (restored == null || restored.size() != result.size() || !restoredWidths.equals(this.glyphsByWidth)) {
			return false;
		}
		for (int i = 0; i < restored.size(); i++) {
			if (restored.get(i) != result.get(i)) {
				return false;
			}
		}
		return true;
	}
}
