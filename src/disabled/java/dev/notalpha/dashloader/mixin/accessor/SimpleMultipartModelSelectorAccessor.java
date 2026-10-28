package dev.notalpha.dashloader.mixin.accessor;

import net.minecraft.client.renderer.block.dispatch.multipart.KeyValueCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyValueCondition.class)
public interface SimpleMultipartModelSelectorAccessor {
	@Accessor
	String getKey();

	@Accessor
	String getValueString();
}
