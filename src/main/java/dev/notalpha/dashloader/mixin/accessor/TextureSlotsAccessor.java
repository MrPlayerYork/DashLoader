package dev.notalpha.dashloader.mixin.accessor;

import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(TextureSlots.class)
public interface TextureSlotsAccessor {
	@Accessor("resolvedValues")
	Map<String, Material> getResolvedValues();
}
