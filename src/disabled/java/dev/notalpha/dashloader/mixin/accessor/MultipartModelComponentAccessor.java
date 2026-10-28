package dev.notalpha.dashloader.mixin.accessor;

import net.minecraft.client.renderer.block.dispatch.multipart.Selector;
import net.minecraft.client.renderer.block.dispatch.multipart.Condition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Selector.class)
public interface MultipartModelComponentAccessor {
	@Accessor()
	Condition getSelector();
}
