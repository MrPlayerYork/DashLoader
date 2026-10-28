package dev.notalpha.dashloader.mixin.option.cache.model;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.model.ModelGroupModule;
import dev.notalpha.dashloader.misc.DebugHooks;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelGroupCollector;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModelGroupCollector.class)
public class ModelGroupMixin {
	@WrapMethod(method = "build(Lnet/minecraft/client/color/block/BlockColors;Lnet/minecraft/client/resources/model/BlockStateModelLoader$LoadedModels;)Lit/unimi/dsi/fastutil/objects/Object2IntMap;")
	private static Object2IntMap<BlockState> dashloaderBuild(BlockColors colors, BlockStateModelLoader.LoadedModels models, Operation<Object2IntMap<BlockState>> original) {
		Object2IntMap<BlockState> groups = dashloaderGroups(colors, models, original);
		if (DebugHooks.enabled()) {
			// Group numbers are handed out in an arbitrary order, so compare which block states share a group instead.
			long h = 17;
			var sorted = new java.util.TreeMap<String, Integer>();
			groups.forEach((state, group) -> sorted.put(ModelGroupModule.keyOf(state), group));
			var relabelled = new java.util.HashMap<Integer, Integer>();
			for (var e : sorted.entrySet()) {
				int label = relabelled.computeIfAbsent(e.getValue(), g -> relabelled.size());
				h = h * 31 + e.getKey().hashCode();
				h = h * 31 + label;
			}
			DebugHooks.record("groups", "all", h * 31 + sorted.size());
		}
		return groups;
	}

	private static Object2IntMap<BlockState> dashloaderGroups(BlockColors colors, BlockStateModelLoader.LoadedModels models, Operation<Object2IntMap<BlockState>> original) {
		var load = ModelGroupModule.LOAD_GROUPS.get(CacheStatus.LOAD);
		if (load != null) {
			Object2IntMap<BlockState> restored = load.restore();
			if (restored != null) {
				dev.notalpha.dashloader.DashLoader.LOG.info("Model groups: {} block states restored from the cache", restored.size());
				return restored;
			}
			dev.notalpha.dashloader.DashLoader.LOG.info("Model groups from the cache could not be used (blocks changed or no data), computing them");
		}

		Object2IntMap<BlockState> result = original.call(colors, models);
		var save = ModelGroupModule.SAVE_GROUPS.get(CacheStatus.SAVE);
		if (save != null) {
			save.set(ModelGroupModule.capture(result));
		}
		return result;
	}
}
