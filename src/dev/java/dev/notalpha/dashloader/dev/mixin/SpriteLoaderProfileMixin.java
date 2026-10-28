package dev.notalpha.dashloader.dev.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.notalpha.dashloader.dev.StageProfiler;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(SpriteLoader.class)
public class SpriteLoaderProfileMixin {
	@WrapMethod(method = "stitch")
	private SpriteLoader.Preparations profileStitch(List<SpriteContents> sprites, int mipLevel, Executor executor, Operation<SpriteLoader.Preparations> original) {
		long start = StageProfiler.begin();
		SpriteLoader.Preparations out = original.call(sprites, mipLevel, executor);
		StageProfiler.end("SpriteLoader.stitch (mip " + mipLevel + ", " + sprites.size() + " sprites)", start);
		return out;
	}
}
