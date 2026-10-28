package dev.notalpha.dashloader.mixin.accessor;

import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BitmapProvider.class)
public interface BitmapFontAccessor {
	@Invoker("<init>")
	static BitmapProvider init(BitmapProvider.ImageDataHolder imageData, CodepointMap<BitmapProvider.Glyph> glyphs) {
		throw new AssertionError();
	}

	@Accessor
	CodepointMap<BitmapProvider.Glyph> getGlyphs();

	@Accessor("imageData")
	BitmapProvider.ImageDataHolder getImageData();
}
