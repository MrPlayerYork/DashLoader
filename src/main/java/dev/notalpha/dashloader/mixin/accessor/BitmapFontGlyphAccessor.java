package dev.notalpha.dashloader.mixin.accessor;

import net.minecraft.client.gui.font.providers.BitmapProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BitmapProvider.Glyph.class)
public interface BitmapFontGlyphAccessor {
	@Invoker("<init>")
	static BitmapProvider.Glyph init(float scale, BitmapProvider.ImageDataHolder imageData, int offsetX, int offsetY, int width, int height, int advance, int ascent) {
		throw new AssertionError();
	}
}
