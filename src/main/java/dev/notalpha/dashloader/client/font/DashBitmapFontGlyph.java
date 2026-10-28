package dev.notalpha.dashloader.client.font;

import dev.notalpha.dashloader.mixin.accessor.BitmapFontGlyphAccessor;
import net.minecraft.client.gui.font.providers.BitmapProvider;

public final class DashBitmapFontGlyph {
	public final float scale;
	public final int offsetX;
	public final int offsetY;
	public final int width;
	public final int height;
	public final int advance;
	public final int ascent;

	public DashBitmapFontGlyph(float scale, int offsetX, int offsetY, int width, int height, int advance, int ascent) {
		this.scale = scale;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.width = width;
		this.height = height;
		this.advance = advance;
		this.ascent = ascent;
	}

	public DashBitmapFontGlyph(BitmapProvider.Glyph glyph) {
		this.scale = glyph.scale();
		this.offsetX = glyph.offsetX();
		this.offsetY = glyph.offsetY();
		this.width = glyph.width();
		this.height = glyph.height();
		this.advance = glyph.advance();
		this.ascent = glyph.ascent();
	}

	public BitmapProvider.Glyph export(BitmapProvider.ImageDataHolder imageData) {
		return BitmapFontGlyphAccessor.init(this.scale, imageData, this.offsetX, this.offsetY, this.width, this.height, this.advance, this.ascent);
	}
}
