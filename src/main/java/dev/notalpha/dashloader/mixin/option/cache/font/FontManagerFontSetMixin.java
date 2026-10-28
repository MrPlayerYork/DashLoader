package dev.notalpha.dashloader.mixin.option.cache.font;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.font.GlyphProvider;
import dev.notalpha.dashloader.client.font.FontSetModule;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.Set;

/**
 * Tells {@link FontSetSelectMixin} which font is being created.
 */
@Mixin(FontManager.class)
public class FontManagerFontSetMixin {
	@WrapMethod(method = "createFontSet(Lnet/minecraft/resources/Identifier;Ljava/util/List;Ljava/util/Set;)Lnet/minecraft/client/gui/font/FontSet;")
	private FontSet dashloaderCreateFontSet(Identifier name, List<GlyphProvider.Conditional> providers, Set<FontOption> options, Operation<FontSet> original) {
		FontSetModule.CURRENT_FONT.set(name.toString());
		try {
			return original.call(name, providers, options);
		} finally {
			FontSetModule.CURRENT_FONT.remove();
		}
	}
}
