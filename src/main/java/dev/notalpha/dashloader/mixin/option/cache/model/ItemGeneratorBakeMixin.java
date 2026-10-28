package dev.notalpha.dashloader.mixin.option.cache.model;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.notalpha.dashloader.client.model.geometry.GeometryCaching;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Generated item models (flat items with sides extruded from the pixels of the sprite). Scanning the pixels of every item is
 * a large part of the model baking.
 */
@Mixin(ItemModelGenerator.class)
public class ItemGeneratorBakeMixin {
	@WrapMethod(method = "bake(Lnet/minecraft/client/resources/model/sprite/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/geometry/QuadCollection;")
	private static QuadCollection dashloaderBake(TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name, Operation<QuadCollection> original) {
		return GeometryCaching.bake(null, slots, baker, state, name, () -> original.call(slots, baker, state, name));
	}
}
