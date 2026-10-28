package dev.notalpha.dashloader.mixin.accessor;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BitmapProvider.ImageDataHolder.class)
public interface BitmapImageDataHolderAccessor {
	@Invoker("<init>")
	static BitmapProvider.ImageDataHolder init(Identifier identifier, NativeImage image) {
		throw new AssertionError();
	}

	@Accessor("identifier")
	Identifier getIdentifier();

	@Accessor("image")
	NativeImage getImage();
}
