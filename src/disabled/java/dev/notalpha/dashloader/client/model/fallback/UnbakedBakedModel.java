package dev.notalpha.dashloader.client.model.fallback;

import dev.notalpha.dashloader.client.Dazy;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;

/**
 * An unbaked model which holds a baked model, used for fallback to reuse cached models.
 */
public class UnbakedBakedModel implements UnbakedModel {
	private final Dazy<? extends BakedModel> bakedModel;

	public UnbakedBakedModel(Dazy<? extends BakedModel> bakedModel) {
		this.bakedModel = bakedModel;
	}

	@Override
	public void resolve(Resolver resolver) {
	}

	@Override
	public BakedModel bake(TextureSlots textures, ModelBaker baker, ModelState settings, boolean ambientOcclusion, boolean isSideLit, ItemTransforms transformation) {
		return this.bakedModel.get(baker.getSpriteGetter());
	}
}
