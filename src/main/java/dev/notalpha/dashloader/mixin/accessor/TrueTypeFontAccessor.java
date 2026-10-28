package dev.notalpha.dashloader.mixin.accessor;

import net.minecraft.client.gui.font.CodepointMap;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import org.lwjgl.util.freetype.FT_Face;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.ByteBuffer;

@Mixin(TrueTypeGlyphProvider.class)
public interface TrueTypeFontAccessor {
	@Accessor("fontMemory")
	@Mutable
	void setBuffer(ByteBuffer thing);

	@Accessor
	FT_Face getFace();

	@Accessor
	@Mutable
	void setFace(FT_Face thing);

	@Accessor
	float getOversample();

	@Accessor
	@Mutable
	void setOversample(float thing);

	@Accessor("glyphs")
	@Mutable
	void setContainer(CodepointMap<?> container);

	@Accessor("glyphs")
	CodepointMap<?> getContainer();
}
