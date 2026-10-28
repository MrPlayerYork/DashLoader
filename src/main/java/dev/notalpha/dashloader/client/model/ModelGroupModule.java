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
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Block states which look the same are put in groups when the models are loaded, a single threaded step which only depends on
 * the models. This saves the result, indexed by the global id of the block states.
 * <br><br>
 * The ids are only used when the blocks and their states are exactly the same as when the cache was made, which is checked with
 * {@link #registryChecksum()}.
 */
public class ModelGroupModule implements DashModule<ModelGroupModule.Data> {
	public static final CachingData<AtomicReference<Data>> SAVE_GROUPS = new CachingData<>(CacheStatus.SAVE);
	public static final CachingData<Data> LOAD_GROUPS = new CachingData<>(CacheStatus.LOAD);

	/**
	 * A fingerprint of every block, in registry order, and how many states it has.
	 */
	public static long registryChecksum() {
		long hash = 1125899906842597L;
		for (Block block : BuiltInRegistries.BLOCK) {
			hash = 31 * hash + BuiltInRegistries.BLOCK.getKey(block).toString().hashCode();
			hash = 31 * hash + block.getStateDefinition().getPossibleStates().size();
		}
		return 31 * hash + Block.BLOCK_STATE_REGISTRY.size();
	}

	/**
	 * @return null if a block state in there can't be told apart in the next session.
	 */
	@Nullable
	public static Data capture(Object2IntMap<BlockState> groups) {
		int[] states = new int[groups.size()];
		int[] values = new int[groups.size()];
		int i = 0;
		for (Object2IntMap.Entry<BlockState> entry : groups.object2IntEntrySet()) {
			states[i] = codeOf(entry.getKey());
			if (states[i] == UNKNOWN) {
				DashLoader.LOG.info("Not caching the model groups, {} can't be identified.", entry.getKey());
				return null;
			}
			values[i] = entry.getIntValue();
			i++;
		}
		return new Data(registryChecksum(), groups.defaultReturnValue(), states, values);
	}

	private static final int UNKNOWN = Integer.MIN_VALUE;

	/**
	 * Block states of blocks have a global id. The item frame models use four stand in states which are not part of the registry,
	 * they get the codes -2 to -5.
	 */
	public static int codeOf(BlockState state) {
		int id = Block.BLOCK_STATE_REGISTRY.getId(state);
		if (id >= 0) {
			return id;
		}
		for (int code = 0; code < 4; code++) {
			if (BlockStateDefinitions.getItemFrameFakeState((code & 2) != 0, (code & 1) != 0) == state) {
				return -2 - code;
			}
		}
		return UNKNOWN;
	}

	@Nullable
	private static BlockState stateOf(int code) {
		if (code >= 0) {
			return Block.BLOCK_STATE_REGISTRY.byId(code);
		}
		int fake = -2 - code;
		return fake >= 0 && fake < 4 ? BlockStateDefinitions.getItemFrameFakeState((fake & 2) != 0, (fake & 1) != 0) : null;
	}

	@Override
	public void reset(Cache cache) {
		SAVE_GROUPS.reset(cache, new AtomicReference<>());
		LOAD_GROUPS.reset(cache, () -> new Data(0, 0, new int[0], new int[0]));
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
		public final int[] states;
		public final int[] groups;

		public Data(long checksum, int defaultValue, int[] states, int[] groups) {
			this.checksum = checksum;
			this.defaultValue = defaultValue;
			this.states = states;
			this.groups = groups;
		}

		/** Null if the blocks of the game are not the ones this was made for. */
		@Nullable
		public Object2IntMap<BlockState> restore() {
			if (this.states.length == 0 || this.checksum != registryChecksum()) {
				return null;
			}
			Object2IntMap<BlockState> out = new Object2IntOpenHashMap<>(this.states.length);
			out.defaultReturnValue(this.defaultValue);
			for (int i = 0; i < this.states.length; i++) {
				BlockState state = stateOf(this.states[i]);
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
