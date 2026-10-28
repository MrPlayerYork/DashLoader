package dev.notalpha.dashloader.dev.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.notalpha.dashloader.dev.StageProfiler;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelGroupCollector;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModelGroupCollector.class)
public class ModelGroupProfileMixin {
	@WrapMethod(method = "build")
	private static Object2IntMap<BlockState> profileBuild(BlockColors colors, BlockStateModelLoader.LoadedModels models, Operation<Object2IntMap<BlockState>> original) {
		long start = StageProfiler.begin();
		Object2IntMap<BlockState> out = original.call(colors, models);
		StageProfiler.end("ModelGroupCollector.build", start);
		return out;
	}
}
