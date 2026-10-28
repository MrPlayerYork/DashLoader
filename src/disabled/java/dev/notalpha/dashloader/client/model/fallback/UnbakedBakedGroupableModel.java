package dev.notalpha.dashloader.client.model.fallback;

import dev.notalpha.dashloader.client.Dazy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;

/**
 * Same as {@linkplain UnbakedBakedModel}, but for {@linkplain UnbakedBlockStateModel}
 */
public class UnbakedBakedGroupableModel implements UnbakedBlockStateModel {
	private final Dazy<? extends BakedModel> bakedModel;

	public UnbakedBakedGroupableModel(Dazy<? extends BakedModel> bakedModel) {
		this.bakedModel = bakedModel;
	}

	@Override
	public BakedModel bake(ModelBaker baker) {
		return this.bakedModel.get(baker.getSpriteGetter());
	}

	@Override
	public Object getEqualityGroup(BlockState state) {
		return null;
	}

	@Override
	public void resolve(Resolver resolver) {

	}
}
