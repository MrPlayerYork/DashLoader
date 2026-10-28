package dev.notalpha.dashloader.mixin.profile;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.notalpha.dashloader.misc.StageProfiler;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ClientItemInfoLoader.class)
public class ClientItemInfoProfileMixin {
	@WrapMethod(method = "scheduleLoad")
	private static CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> profileLoad(ResourceManager manager, Executor executor, Operation<CompletableFuture<ClientItemInfoLoader.LoadedClientInfos>> original) {
		long start = StageProfiler.begin();
		return StageProfiler.track("ClientItemInfoLoader.scheduleLoad (item json)", start, original.call(manager, executor));
	}
}
