package dev.notalpha.dashloader.mixin.profile;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.notalpha.dashloader.misc.StageProfiler;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FontManager.class)
public class FontManagerProfileMixin {
	@WrapMethod(method = "apply")
	private void profileApply(FontManager.Preparation preparation, ProfilerFiller profiler, Operation<Void> original) {
		long start = StageProfiler.begin();
		original.call(preparation, profiler);
		StageProfiler.end("FontManager.apply (main thread)", start);
	}
}
