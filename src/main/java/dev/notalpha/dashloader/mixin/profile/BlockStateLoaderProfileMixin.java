package dev.notalpha.dashloader.mixin.profile;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.notalpha.dashloader.misc.StageProfiler;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(BlockStateModelLoader.class)
public class BlockStateLoaderProfileMixin {
	@WrapMethod(method = "loadBlockStates")
	private static CompletableFuture<BlockStateModelLoader.LoadedModels> profileLoad(ResourceManager manager, Executor executor, Operation<CompletableFuture<BlockStateModelLoader.LoadedModels>> original) {
		long start = StageProfiler.begin();
		return StageProfiler.track("BlockStateModelLoader.loadBlockStates (blockstate json)", start, original.call(manager, executor));
	}
}
