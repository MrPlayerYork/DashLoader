package dev.notalpha.dashloader.mixin.accessor;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {
	@Accessor("originalImage")
	NativeImage getImage();

	@Accessor("animatedTexture")
	SpriteContents.AnimatedTexture getAnimation();

	@Accessor("animatedTexture")
	@Mutable
	void setAnimation(SpriteContents.AnimatedTexture animation);

	@Accessor("additionalMetadata")
	@Mutable
	void setAdditionalMetadata(List<MetadataSectionType.WithValue<?>> additionalMetadata);

	@Accessor("mipmapStrategy")
	@Mutable
	void setMipmapStrategy(MipmapStrategy mipmapStrategy);

	@Accessor("alphaCutoffBias")
	@Mutable
	void setAlphaCutoffBias(float alphaCutoffBias);
}
