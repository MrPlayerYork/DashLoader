package dev.notalpha.dashloader.misc.filter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Answers "does any of these patterns match somewhere in this path" ({@link Pattern#asPredicate()}, an empty optional matches
 * everything), the way the resource pack filters of Minecraft do, but without trying every pattern on every path.
 * <br><br>
 * Resource packs can block hundreds of paths with regular expressions, and every resource that is listed or opened is checked against
 * all of them, many times per reload. Here the text each expression needs is searched for once per path, and an expression is only
 * run when all of its text is there. The answers are the same as with running them all.
 */
public final class PathFilterIndex {
	private static final int MEMO_LIMIT = 1 << 18;

	private final boolean matchesEverything;
	/** Patterns for which nothing is known about the text they need. */
	private final Pattern[] always;
	private final Pattern[] indexed;
	/** For each indexed pattern, the numbers of the literals it needs. */
	private final int[][] needs;
	private final LiteralSearch search;
	private final Map<String, Boolean> memo = new ConcurrentHashMap<>();

	public PathFilterIndex(List<Optional<Pattern>> patterns) {
		boolean everything = false;
		List<Pattern> always = new ArrayList<>();
		List<Pattern> indexed = new ArrayList<>();
		List<int[]> needs = new ArrayList<>();
		Map<String, Integer> ids = new HashMap<>();
		for (Optional<Pattern> optional : patterns) {
			if (optional.isEmpty()) {
				everything = true;
				continue;
			}
			Pattern pattern = optional.get();
			List<String> literals = RequiredLiterals.of(pattern);
			if (literals.isEmpty()) {
				always.add(pattern);
				continue;
			}
			int[] required = new int[literals.size()];
			for (int i = 0; i < required.length; i++) {
				required[i] = ids.computeIfAbsent(literals.get(i), key -> ids.size());
			}
			indexed.add(pattern);
			needs.add(required);
		}
		this.matchesEverything = everything;
		this.always = always.toArray(new Pattern[0]);
		this.indexed = indexed.toArray(new Pattern[0]);
		this.needs = needs.toArray(new int[0][]);
		List<String> literals = new ArrayList<>(ids.size());
		for (int i = 0; i < ids.size(); i++) {
			literals.add(null);
		}
		ids.forEach((literal, id) -> literals.set(id, literal));
		this.search = new LiteralSearch(literals);
	}

	public boolean matches(String path) {
		if (this.matchesEverything) {
			return true;
		}
		Boolean known = this.memo.get(path);
		if (known != null) {
			return known;
		}
		boolean result = this.compute(path);
		if (this.memo.size() < MEMO_LIMIT) {
			this.memo.put(path, result);
		}
		return result;
	}

	private boolean compute(String path) {
		for (Pattern pattern : this.always) {
			if (pattern.matcher(path).find()) {
				return true;
			}
		}
		if (this.indexed.length == 0) {
			return false;
		}
		BitSet present = this.search.search(path);
		if (present.isEmpty()) {
			return false;
		}
		candidates:
		for (int i = 0; i < this.indexed.length; i++) {
			for (int id : this.needs[i]) {
				if (!present.get(id)) {
					continue candidates;
				}
			}
			if (this.indexed[i].matcher(path).find()) {
				return true;
			}
		}
		return false;
	}
}
