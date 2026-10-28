package dev.notalpha.dashloader.client.font;

import dev.notalpha.dashloader.api.DashObject;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.api.registry.RegistryReader;
import dev.notalpha.dashloader.io.IOHelper;
import dev.notalpha.dashloader.misc.UnsafeHelper;
import dev.notalpha.dashloader.mixin.accessor.TrueTypeFontAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Optional;

public final class DashTrueTypeFont implements DashObject<TrueTypeGlyphProvider, TrueTypeGlyphProvider> {
	public final byte[] fontData;
	public final float oversample;
	public final int[] excludedCharacters;
	public final float size;
	public final float shiftX;
	public final float shiftY;
	private transient TrueTypeGlyphProvider _font;

	public DashTrueTypeFont(byte[] fontData, float oversample, int[] excludedCharacters, float size, float shiftX, float shiftY) {
		this.fontData = fontData;
		this.oversample = oversample;
		this.excludedCharacters = excludedCharacters;
		this.size = size;
		this.shiftX = shiftX;
		this.shiftY = shiftY;
	}

	public DashTrueTypeFont(TrueTypeGlyphProvider font) {
		TrueTypeFontAccessor fontAccess = (TrueTypeFontAccessor) font;
		FT_Face ft_face = fontAccess.getFace();
		FontPrams prams = FontModule.FONT_TO_DATA.get(CacheStatus.SAVE).get(ft_face);
		final Identifier ttFont = prams.id();
		byte[] data = null;
		try {
			Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(ttFont.withPrefix("font/"));
			if (resource.isPresent()) {
				var stream = resource.get().open();
				data = IOHelper.streamToArray(stream);
				stream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (MemoryStack memoryStack = MemoryStack.stackPush()) {
			FT_Vector vec = FT_Vector.malloc(memoryStack);
			FreeType.FT_Get_Transform(ft_face, null, vec);
			this.shiftX = vec.x() / 64F;
			this.shiftY = vec.y() / 64F;
		}

		this.fontData = data;
		this.oversample = fontAccess.getOversample();
		this.excludedCharacters = prams.skip().codePoints().toArray();
		this.size = prams.size();
	}

	@Override
	public TrueTypeGlyphProvider export(RegistryReader handler) {
		this._font = UnsafeHelper.allocateInstance(TrueTypeGlyphProvider.class);

		TrueTypeFontAccessor trueTypeFontAccess = (TrueTypeFontAccessor) this._font;
		trueTypeFontAccess.setOversample(this.oversample);
		return this._font;
	}

	@Override
	public void postExport(RegistryReader reader) {
		ByteBuffer fontBuffer = MemoryUtil.memAlloc(this.fontData.length);
		fontBuffer.put(this.fontData);
		fontBuffer.flip();

		FT_Face ft_face = null;

		try {
			synchronized (FreeTypeUtil.LIBRARY_LOCK) {
				try (MemoryStack memoryStack = MemoryStack.stackPush()) {
					PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
					FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), fontBuffer, 0L, pointerBuffer), "Initializing font face");
					ft_face = FT_Face.create(pointerBuffer.get());
				}
			}

			// The constructor sets up the face and indexes the glyphs, its state is moved over to the instance that has already been handed out.
			var skip = new String(this.excludedCharacters, 0, this.excludedCharacters.length);
			var built = new TrueTypeGlyphProvider(fontBuffer, ft_face, this.size, this.oversample, this.shiftX, this.shiftY, skip);

			var trueTypeFontAccess = (TrueTypeFontAccessor) this._font;
			trueTypeFontAccess.setContainer(((TrueTypeFontAccessor) built).getContainer());
			trueTypeFontAccess.setFace(ft_face);
			trueTypeFontAccess.setBuffer(fontBuffer);
		} catch (Throwable e) {
			synchronized (FreeTypeUtil.LIBRARY_LOCK) {
				if (ft_face != null) {
					FreeType.FT_Done_Face(ft_face);
				}
			}
			MemoryUtil.memFree(fontBuffer);

			throw e;
		}
	}

	public record FontPrams(Identifier id, float size, String skip) {
	}
}
