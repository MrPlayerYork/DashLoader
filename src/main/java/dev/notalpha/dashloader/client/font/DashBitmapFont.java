package dev.notalpha.dashloader.client.font;

import dev.notalpha.dashloader.api.DashObject;
import dev.notalpha.dashloader.api.collection.IntObjectList;
import dev.notalpha.dashloader.api.registry.RegistryReader;
import dev.notalpha.dashloader.api.registry.RegistryWriter;
import dev.notalpha.dashloader.mixin.accessor.BitmapFontAccessor;
import dev.notalpha.dashloader.mixin.accessor.BitmapImageDataHolderAccessor;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.providers.BitmapProvider;

import java.util.ArrayList;

public final class DashBitmapFont implements DashObject<BitmapProvider, BitmapProvider> {
	public final int identifier;
	public final int image;
	public final IntObjectList<DashBitmapFontGlyph> glyphs;

	public DashBitmapFont(int identifier,
	                      int image,
	                      IntObjectList<DashBitmapFontGlyph> glyphs) {
		this.identifier = identifier;
		this.image = image;
		this.glyphs = glyphs;
	}

	public DashBitmapFont(BitmapProvider bitmapFont, RegistryWriter writer) {
		BitmapFontAccessor font = ((BitmapFontAccessor) bitmapFont);
		var imageData = (BitmapImageDataHolderAccessor) (Object) font.getImageData();
		this.identifier = writer.add(imageData.getIdentifier());
		this.image = writer.add(imageData.getImage());
		this.glyphs = new IntObjectList<>(new ArrayList<>());
		font.getGlyphs().forEach((integer, bitmapFontGlyph) -> this.glyphs.put(integer, new DashBitmapFontGlyph(bitmapFontGlyph)));
	}

	public BitmapProvider export(RegistryReader reader) {
		// The glyphs share the image holder with the provider.
		var imageData = BitmapImageDataHolderAccessor.init(reader.get(this.identifier), reader.get(this.image));
		CodepointMap<BitmapProvider.Glyph> out = new CodepointMap<>(
				BitmapProvider.Glyph[]::new,
				BitmapProvider.Glyph[][]::new
		);
		this.glyphs.forEach((key, value) -> out.put(key, value.export(imageData)));
		return BitmapFontAccessor.init(imageData, out);
	}
}
