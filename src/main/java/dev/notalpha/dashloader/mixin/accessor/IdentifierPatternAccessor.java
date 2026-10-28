package dev.notalpha.dashloader.mixin.accessor;

import net.minecraft.util.IdentifierPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;
import java.util.regex.Pattern;

@Mixin(IdentifierPattern.class)
public interface IdentifierPatternAccessor {
	@Accessor("pathPattern")
	Optional<Pattern> getPathPattern();

	@Accessor("namespacePattern")
	Optional<Pattern> getNamespacePattern();
}
