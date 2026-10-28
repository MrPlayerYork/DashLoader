package dev.notalpha.dashloader.mixin.profile;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.notalpha.dashloader.misc.StageProfiler;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.LoadedBlockModels;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Timing for the stages of ModelManager's reload, to see where the time goes.
 */
@Mixin(ModelManager.class)
public class ModelManagerProfileMixin {
	@WrapMethod(method = "loadBlockModels")
	private static CompletableFuture<Map<Identifier, UnbakedModel>> profileLoadBlockModels(ResourceManager manager, Executor executor, Operation<CompletableFuture<Map<Identifier, UnbakedModel>>> original) {
		long start = StageProfiler.begin();
		return StageProfiler.track("ModelManager.loadBlockModels (model json)", start, original.call(manager, executor));
	}

	@WrapMethod(method = "discoverModelDependencies")
	private static ModelManager.ResolvedModels profileDiscover(Map<Identifier, UnbakedModel> models, BlockStateModelLoader.LoadedModels blockStates, ClientItemInfoLoader.LoadedClientInfos clientInfos, Operation<ModelManager.ResolvedModels> original) {
		long start = StageProfiler.begin();
		ModelManager.ResolvedModels out = original.call(models, blockStates, clientInfos);
		StageProfiler.end("ModelManager.discoverModelDependencies", start);
		return out;
	}

	@WrapMethod(method = "loadModels")
	private static CompletableFuture<?> profileLoadModels(SpriteLoader.Preparations blocks, SpriteLoader.Preparations items, ModelBakery bakery, LoadedBlockModels loaded, Object2IntMap<BlockState> groups, EntityModelSet entityModels, Executor executor, Operation<CompletableFuture<?>> original) {
		long start = StageProfiler.begin();
		return StageProfiler.track("ModelManager.loadModels (bake)", start, original.call(blocks, items, bakery, loaded, groups, entityModels, executor));
	}

	@WrapMethod(method = "apply")
	private void profileApply(ModelManager.ReloadState state, Operation<Void> original) {
		long start = StageProfiler.begin();
		original.call(state);
		StageProfiler.end("ModelManager.apply (main thread)", start);
	}
}
