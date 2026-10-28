package dev.notalpha.dashloader.mixin.option.cache.sprite.content;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Transparency;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.sprite.content.MipmapModule;
import dev.notalpha.dashloader.misc.StateDump;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MipmapGenerator.class)
public class MipmapGenerateMixin {
	@WrapMethod(method = "generateMipLevels(Lnet/minecraft/resources/Identifier;[Lcom/mojang/blaze3d/platform/NativeImage;ILnet/minecraft/client/renderer/texture/MipmapStrategy;FLcom/mojang/blaze3d/platform/Transparency;)[Lcom/mojang/blaze3d/platform/NativeImage;")
	private static NativeImage[] dashloaderGenerate(Identifier name, NativeImage[] images, int level, MipmapStrategy strategy, float alphaCutoffBias, Transparency transparency, Operation<NativeImage[]> original) {
		NativeImage[] out = dashloaderGenerateInner(name, images, level, strategy, alphaCutoffBias, transparency, original);
		if (StateDump.enabled() && images.length == 1) {
			StateDump.record("mip", name + "|" + level + "|" + strategy + "|" + Float.floatToIntBits(alphaCutoffBias) + "|" + transparency, StateDump.hash(out));
		}
		return out;
	}

	private static NativeImage[] dashloaderGenerateInner(Identifier name, NativeImage[] images, int level, MipmapStrategy strategy, float alphaCutoffBias, Transparency transparency, Operation<NativeImage[]> original) {
		var load = MipmapModule.LOAD_LEVELS.get(CacheStatus.LOAD);
		var save = MipmapModule.SAVE_LEVELS.get(CacheStatus.SAVE);
		if (load != null && images.length == 1 && level == 0 && MipmapModule.FINISHED_IMAGES.remove(images[0])) {
			// Nothing to generate and the image is already in the state the generation would leave it in.
			MipmapModule.SKIPPED.incrementAndGet();
			return images;
		}
		// Only the first generation from the plain image is cached, which is how the sprites get their levels.
		if ((load == null && save == null) || images.length != 1 || level <= 0) {
			return original.call(name, images, level, strategy, alphaCutoffBias, transparency);
		}

		String key = name + "|" + level + "|" + strategy + "|" + Float.floatToIntBits(alphaCutoffBias) + "|" + transparency + "|" + images[0].getWidth() + "x" + images[0].getHeight();
		if (load != null) {
			NativeImage[] cached = load.remove(key);
			if (cached != null && cached.length == level + 1 && matches(images[0], cached[0])) {
				NativeImage[] out = new NativeImage[cached.length];
				out[0] = images[0];
				System.arraycopy(cached, 1, out, 1, cached.length - 1);
				if (cached[0] != images[0]) {
					// The sprite did not come from the cache, give it the pixels the generation leaves in it.
					images[0].copyFrom(cached[0]);
					cached[0].close();
				}
				MipmapModule.HITS.incrementAndGet();
				return out;
			}
			if (MipmapModule.MISSES.incrementAndGet() <= 5) {
				dev.notalpha.dashloader.DashLoader.LOG.info("Mip levels not in the cache: {}", key);
			}
		}

		NativeImage[] result = original.call(name, images, level, strategy, alphaCutoffBias, transparency);
		if (save != null && result.length == level + 1 && result[0] == images[0]) {
			save.putIfAbsent(key, result);
		}
		return result;
	}

	private static boolean matches(NativeImage image, NativeImage cached) {
		return image.getWidth() == cached.getWidth() && image.getHeight() == cached.getHeight() && image.format() == cached.format();
	}
}
