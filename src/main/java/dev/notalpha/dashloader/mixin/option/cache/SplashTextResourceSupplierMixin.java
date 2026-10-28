package dev.notalpha.dashloader.mixin.option.cache;

import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.splash.SplashModule;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SplashManager.class)
public abstract class SplashTextResourceSupplierMixin {
	@Shadow
	private static Component literalSplash(String text) {
		throw new AssertionError();
	}

	// Splashes are plain literal components in the same style, so the cache only needs their text.
	@Inject(
			method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/List;",
			at = @At(value = "HEAD"),
			cancellable = true
	)
	private void applySplashCache(ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<List<Component>> cir) {
		SplashModule.TEXTS.visit(CacheStatus.LOAD, strings -> cir.setReturnValue(strings.stream().map(SplashTextResourceSupplierMixin::literalSplash).toList()));
	}

	@Inject(
			method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/List;",
			at = @At(value = "RETURN")
	)
	private void stealSplashCache(ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<List<Component>> cir) {
		SplashModule.TEXTS.visit(CacheStatus.SAVE, strings -> {
			strings.clear();
			for (Component splash : cir.getReturnValue()) {
				strings.add(splash.getString());
			}
		});
	}
}
