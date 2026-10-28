package dev.notalpha.dashloader.mixin.option.cache.sprite.content;

import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.sprite.content.SpriteContentModule;
import dev.notalpha.dashloader.mixin.accessor.SpriteContentsAccessor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Set;

@Mixin(SpriteResourceLoader.class)
public interface SpriteOpenerMixin {
	// SpriteResourceLoader.create(Set) returns a lambda that reads the metadata and then decodes the image.
	@Inject(
			method = "lambda$create$0",
			cancellable = true,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;open()Ljava/io/InputStream;", shift = At.Shift.BEFORE)
	)
	private static void dashloaderLoad(Set<MetadataSectionType<?>> metadatas, Identifier id, Resource resource, CallbackInfoReturnable<SpriteContents> cir) {
		var dashSpriteData = SpriteContentModule.SOURCE.get(CacheStatus.LOAD);
		if (dashSpriteData != null) {
			SpriteContents spriteContents = dashSpriteData.get(id);
			if (spriteContents != null) {
				// Same metadata handling as the SpriteContents constructor, the image and animation come from the cache.
				ResourceMetadata resourceMetadata;
				try {
					resourceMetadata = resource.metadata();
				} catch (IOException e) {
					return; // let vanilla report the broken metadata
				}
				var access = (SpriteContentsAccessor) spriteContents;
				access.setAdditionalMetadata(resourceMetadata.getTypedSections(metadatas));
				resourceMetadata.getSection(TextureMetadataSection.TYPE).ifPresent(texture -> {
					access.setMipmapStrategy(texture.mipmapStrategy());
					access.setAlphaCutoffBias(texture.alphaCutoffBias());
				});
				cir.setReturnValue(spriteContents);
			}
		}
	}

	@Inject(
			method = "lambda$create$0",
			at = @At(value = "RETURN")
	)
	private static void dashloaderSave(Set<?> metadatas, Identifier id, Resource resource, CallbackInfoReturnable<SpriteContents> cir) {
		var dashSpriteData = SpriteContentModule.SOURCE.get(CacheStatus.SAVE);
		if (dashSpriteData != null) {
			if (dashSpriteData.containsKey(id)) { // filter out sprites with the same id
				dashSpriteData.put(id, null);
				return;
			}
			dashSpriteData.put(id, cir.getReturnValue());
		}
	}
}
