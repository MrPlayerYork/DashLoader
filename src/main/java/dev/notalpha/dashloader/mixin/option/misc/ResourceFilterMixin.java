package dev.notalpha.dashloader.mixin.option.misc;

import dev.notalpha.dashloader.DashLoader;
import dev.notalpha.dashloader.mixin.accessor.IdentifierPatternAccessor;
import dev.notalpha.dashloader.misc.filter.PathFilterIndex;
import net.minecraft.server.packs.resources.ResourceFilterSection;
import net.minecraft.util.IdentifierPattern;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource packs can list hundreds of blocked paths as regular expressions, which the game tries on every path it looks at. This makes
 * that check fast. Short lists are left to the game.
 */
@Mixin(ResourceFilterSection.class)
public class ResourceFilterMixin {
	private static final int MIN_PATTERNS = 8;

	@Shadow
	@Final
	private List<IdentifierPattern> blockList;

	@Unique
	private volatile PathFilterIndex dashloader$paths;
	@Unique
	private volatile ConcurrentHashMap<String, Boolean> dashloader$namespaces;
	@Unique
	private volatile boolean dashloader$failed;

	@Inject(method = "isPathFiltered(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
	private void dashloaderIsPathFiltered(String path, CallbackInfoReturnable<Boolean> cir) {
		if (this.blockList.size() < MIN_PATTERNS || this.dashloader$failed) {
			return;
		}
		try {
			PathFilterIndex index = this.dashloader$paths;
			if (index == null) {
				index = new PathFilterIndex(this.blockList.stream().map(pattern -> ((IdentifierPatternAccessor) pattern).getPathPattern()).toList());
				this.dashloader$paths = index;
			}
			cir.setReturnValue(index.matches(path));
		} catch (RuntimeException e) {
			this.dashloader$failed = true;
			DashLoader.LOG.warn("Could not speed up a resource pack filter, using the normal one.", e);
		}
	}

	@Inject(method = "isNamespaceFiltered(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
	private void dashloaderIsNamespaceFiltered(String namespace, CallbackInfoReturnable<Boolean> cir) {
		if (this.blockList.size() < MIN_PATTERNS) {
			return;
		}
		ConcurrentHashMap<String, Boolean> memo = this.dashloader$namespaces;
		if (memo == null) {
			memo = new ConcurrentHashMap<>();
			this.dashloader$namespaces = memo;
		}
		// Only a handful of namespaces exist, so the answers are kept as they are worked out.
		Boolean known = memo.get(namespace);
		if (known == null) {
			known = this.blockList.stream().anyMatch(pattern -> pattern.namespacePredicate().test(namespace));
			memo.put(namespace, known);
		}
		cir.setReturnValue(known);
	}
}
