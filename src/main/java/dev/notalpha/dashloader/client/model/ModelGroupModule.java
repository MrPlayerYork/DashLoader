package dev.notalpha.dashloader.client.model;

import dev.notalpha.dashloader.DashLoader;
import dev.notalpha.dashloader.api.CachingData;
import dev.notalpha.dashloader.api.DashModule;
import dev.notalpha.dashloader.api.cache.Cache;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.api.registry.RegistryReader;
import dev.notalpha.dashloader.api.registry.RegistryWriter;
import dev.notalpha.dashloader.config.ConfigHandler;
import dev.notalpha.dashloader.config.Option;
import dev.notalpha.taski.builtin.StepTask;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Block states which look the same are put in groups when the models are loaded, a single threaded step which only depends on
 * the models. This saves the result per block state, named by the block and the position of the state in that block's own list.
 * <br><br>
 * The global ids of block states are not used: with many mods the registry order is not the same on every launch. The data is only
 * used when every block is the same as when the cache was made (name, number of states and properties), which is checked with
 * {@link #registryChecksum()}; that check does not depend on the order of the registry.
 */
public class ModelGroupModule implements DashModule<ModelGroupModule.Data> {
	public static final CachingData<AtomicReference<Data>> SAVE_GROUPS = new CachingData<>(CacheStatus.SAVE);
	public static final CachingData<Data> LOAD_GROUPS = new CachingData<>(CacheStatus.LOAD);

	/**
	 * A fingerprint of every block: its name, how many states it has and its properties. The blocks are added up, so the
	 * order of the registry does not matter.
	 */
	public static long registryChecksum() {
		long sum = 0;
		for (Block block : BuiltInRegistries.BLOCK) {
			var definition = block.getStateDefinition();
			long hash = BuiltInRegistries.BLOCK.getKey(block).toString().hashCode();
			hash = 31 * hash + definition.getPossibleStates().size();
			for (Property<?> property : definition.getProperties()) {
				hash = 31 * hash + property.getName().hashCode();
				hash = 31 * hash + property.getPossibleValues().size();
			}
			hash *= 0x9E3779B97F4A7C15L;
			sum += hash ^ (hash >>> 32);
		}
		return sum;
	}

	/**
	 * @return null if a block state in there can't be told apart in the next session.
	 */
	@Nullable
	public static Data capture(Object2IntMap<BlockState> groups) {
		List<String> blocks = new ArrayList<>();
		Object2IntMap<Block> blockIndices = new Object2IntOpenHashMap<>();
		Map<Block, Object2IntMap<BlockState>> localIndices = new HashMap<>();
		int[] stateBlocks = new int[groups.size()];
		int[] stateLocals = new int[groups.size()];
		int[] values = new int[groups.size()];
		int i = 0;
		for (Object2IntMap.Entry<BlockState> entry : groups.object2IntEntrySet()) {
			BlockState state = entry.getKey();
			if (Block.BLOCK_STATE_REGISTRY.getId(state) >= 0) {
				Block block = state.getBlock();
				int blockIndex = blockIndices.computeIfAbsent(block, b -> {
					blocks.add(BuiltInRegistries.BLOCK.getKey(block).toString());
					return blocks.size() - 1;
				});
				var locals = localIndices.computeIfAbsent(block, b -> {
					Object2IntMap<BlockState> map = new Object2IntOpenHashMap<>();
					var all = b.getStateDefinition().getPossibleStates();
					for (int s = 0; s < all.size(); s++) {
						map.put(all.get(s), s);
					}
					return map;
				});
				stateBlocks[i] = blockIndex;
				stateLocals[i] = locals.getInt(state);
			} else {
				stateBlocks[i] = FAKE;
				stateLocals[i] = fakeCode(state);
				if (stateLocals[i] < 0) {
					DashLoader.LOG.info("Not caching the model groups, {} can't be identified.", state);
					return null;
				}
			}
			values[i] = entry.getIntValue();
			i++;
		}
		return new Data(registryChecksum(), groups.defaultReturnValue(), blocks, stateBlocks, stateLocals, values);
	}

	private static final int FAKE = -1;

	/**
	 * The item frame models use four stand in states which are not part of the registry.
	 *
	 * @return 0 to 3, or -1 if this is not one of them
	 */
	private static int fakeCode(BlockState state) {
		for (int code = 0; code < 4; code++) {
			if (BlockStateDefinitions.getItemFrameFakeState((code & 2) != 0, (code & 1) != 0) == state) {
				return code;
			}
		}
		return -1;
	}

	/** A key for a block state which is the same on every launch, for the debug dumps. */
	public static String keyOf(BlockState state) {
		return state.toString();
	}

	@Override
	public void reset(Cache cache) {
		SAVE_GROUPS.reset(cache, new AtomicReference<>());
		LOAD_GROUPS.reset(cache, () -> new Data(0, 0, List.of(), new int[0], new int[0], new int[0]));
	}

	@Override
	public Data save(RegistryWriter writer, StepTask task) {
		var holder = SAVE_GROUPS.get(CacheStatus.SAVE);
		return holder == null ? null : holder.get();
	}

	@Override
	public void load(Data data, RegistryReader reader, StepTask task) {
		LOAD_GROUPS.set(CacheStatus.LOAD, data);
	}

	@Override
	public Class<Data> getDataClass() {
		return Data.class;
	}

	@Override
	public boolean isActive() {
		return ConfigHandler.optionActive(Option.CACHE_MODEL_LOADER);
	}

	public static final class Data {
		public final long checksum;
		public final int defaultValue;
		public final List<String> blocks;
		public final int[] stateBlocks;
		public final int[] stateLocals;
		public final int[] groups;

		public Data(long checksum, int defaultValue, List<String> blocks, int[] stateBlocks, int[] stateLocals, int[] groups) {
			this.checksum = checksum;
			this.defaultValue = defaultValue;
			this.blocks = blocks;
			this.stateBlocks = stateBlocks;
			this.stateLocals = stateLocals;
			this.groups = groups;
		}

		/** Null if the blocks of the game are not the ones this was made for. */
		@Nullable
		public Object2IntMap<BlockState> restore() {
			if (this.groups.length == 0) {
				DashLoader.LOG.info("Model groups: the cache has none saved");
				return null;
			}
			if (this.checksum != registryChecksum()) {
				DashLoader.LOG.info("Model groups: the blocks are not the same as when the cache was made");
				return null;
			}
			Map<String, Block> byName = new HashMap<>();
			for (Block block : BuiltInRegistries.BLOCK) {
				byName.put(BuiltInRegistries.BLOCK.getKey(block).toString(), block);
			}
			Block[] resolved = new Block[this.blocks.size()];
			for (int i = 0; i < resolved.length; i++) {
				resolved[i] = byName.get(this.blocks.get(i));
				if (resolved[i] == null) {
					DashLoader.LOG.warn("Model groups refer to the block {} which does not exist, not using them.", this.blocks.get(i));
					return null;
				}
			}
			Object2IntMap<BlockState> out = new Object2IntOpenHashMap<>(this.groups.length);
			out.defaultReturnValue(this.defaultValue);
			for (int i = 0; i < this.groups.length; i++) {
				BlockState state;
				if (this.stateBlocks[i] == FAKE) {
					int fake = this.stateLocals[i];
					state = fake >= 0 && fake < 4 ? BlockStateDefinitions.getItemFrameFakeState((fake & 2) != 0, (fake & 1) != 0) : null;
				} else {
					var all = resolved[this.stateBlocks[i]].getStateDefinition().getPossibleStates();
					state = this.stateLocals[i] >= 0 && this.stateLocals[i] < all.size() ? all.get(this.stateLocals[i]) : null;
				}
				if (state == null) {
					DashLoader.LOG.warn("Model groups refer to a block state which does not exist, not using them.");
					return null;
				}
				out.put(state, this.groups[i]);
			}
			return out;
		}
	}
}
