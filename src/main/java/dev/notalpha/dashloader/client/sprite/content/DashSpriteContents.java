package dev.notalpha.dashloader.client.sprite.content;

import dev.notalpha.dashloader.api.DashObject;
import dev.notalpha.dashloader.api.registry.RegistryReader;
import dev.notalpha.dashloader.api.registry.RegistryWriter;
import dev.notalpha.dashloader.mixin.accessor.SpriteContentsAccessor;
import dev.notalpha.hyphen.scan.annotations.DataNullable;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;

public final class DashSpriteContents implements DashObject<SpriteContents, SpriteContents> {
	private final static Method sodiumScanMethod = getSodiumScanner();
	public final int id;
	public final int image;
	@Nullable
	@DataNullable
	public final DashSpriteAnimation animation;
	public final int width;
	public final int height;

	public DashSpriteContents(int id, int image, @Nullable DashSpriteAnimation animation, int width, int height) {
		this.id = id;
		this.image = image;
		this.animation = animation;
		this.width = width;
		this.height = height;
	}

	public DashSpriteContents(SpriteContents contents, RegistryWriter writer) {
		var access = (SpriteContentsAccessor) contents;
		this.id = writer.add(contents.name());
		this.image = writer.add(access.getImage());
		this.width = contents.width();
		this.height = contents.height();
		SpriteContents.AnimatedTexture animation = access.getAnimation();
		this.animation = animation == null ? null : new DashSpriteAnimation(animation);
	}

	public SpriteContents export(RegistryReader reader) {
		// The constructor works out the transparency of the image. The metadata dependent parts are filled in by SpriteOpenerMixin.
		NativeImage image = reader.get(this.image);
		MipmapModule.FINISHED_IMAGES.add(image);
		final SpriteContents out = new SpriteContents(reader.get(this.id), new FrameSize(this.width, this.height), image);
		if (this.animation != null) {
			((SpriteContentsAccessor) out).setAnimation(animation.export(out, reader));
		}
		applySodiumScanning(out, image); // run important sodium method if present
		return out;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DashSpriteContents that = (DashSpriteContents) o;

		if (id != that.id) return false;
		if (image != that.image) return false;
		if (width != that.width) return false;
		if (height != that.height) return false;
		return Objects.equals(animation, that.animation);
	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + image;
		result = 31 * result + (animation != null ? animation.hashCode() : 0);
		result = 31 * result + width;
		result = 31 * result + height;
		return result;
	}

	private static Method getSodiumScanner() {
		try {
			Method scanSpriteContents = SpriteContents.class.getDeclaredMethod("scanSpriteContents", NativeImage.class);
			scanSpriteContents.setAccessible(true);
			return scanSpriteContents;
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	private void applySodiumScanning(SpriteContents contents, NativeImage image) {
		if (sodiumScanMethod == null) return;
		try {
			sodiumScanMethod.invoke(contents, image);
		} catch (ReflectiveOperationException ignored) {
		}
	}
}
