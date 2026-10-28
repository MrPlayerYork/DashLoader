package dev.notalpha.dashloader.mixin.option.cache.model;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.notalpha.dashloader.client.model.geometry.GeometryCaching;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * The geometry of cuboid models (everything from block and item model json) goes through this bake.
 */
@Mixin(UnbakedCuboidGeometry.class)
public class GeometryBakeMixin {
	@WrapMethod(method = "bake(Ljava/util/List;Lnet/minecraft/client/resources/model/sprite/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/geometry/QuadCollection;")
	private static QuadCollection dashloaderBake(List<CuboidModelElement> elements, TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name, Operation<QuadCollection> original) {
		return GeometryCaching.bake(elements, slots, baker, state, name, () -> original.call(elements, slots, baker, state, name));
	}
}
