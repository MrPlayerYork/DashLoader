package dev.notalpha.dashloader.misc.filter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathFilterIndexTest {
	private static boolean naive(List<Optional<Pattern>> patterns, String path) {
		return patterns.stream().anyMatch(p -> p.map(Pattern::asPredicate).orElse(s -> true).test(path));
	}

	private static List<Optional<Pattern>> compile(String... regexes) {
		List<Optional<Pattern>> out = new ArrayList<>();
		for (String regex : regexes) {
			out.add(Optional.of(Pattern.compile(regex)));
		}
		return out;
	}

	private static void check(List<Optional<Pattern>> patterns, String... paths) {
		PathFilterIndex index = new PathFilterIndex(patterns);
		for (String path : paths) {
			// Twice, the second answer comes from the memo.
			assertEquals(naive(patterns, path), index.matches(path), path);
			assertEquals(naive(patterns, path), index.matches(path), path);
		}
	}

	@Test
	void packFilterPatterns() {
		var patterns = compile("advancement/recipes/.*/arrow.json", "advancement/recipes/.*/activator_rail.json", "advancement/adventure", "advancement/end");
		check(patterns,
				"advancement/recipes/transportation/arrow.json", "advancement/recipes/arrow.json", "advancement/recipes/x/arrowXjson",
				"advancement/adventure/root.json", "advancement/end/root.json", "textures/block/stone.png", "models/block/arrow.json", "");
	}

	@Test
	void unusualPatterns() {
		var patterns = compile("a(bc|de)f", "(?i)stone", "x[a-c]+yz", "ab?cde", "ab*cde", "ab{2}cde", "foo\\.bar", "fo+bar", "\\Qa.b\\E", "^start", "end$", "w\\wd");
		check(patterns, "abcf", "adef", "STONE", "stone", "xabyz", "xyz", "acde", "abcde", "abbbcde", "abbcde", "foo.bar", "fooxbar", "foobar", "fobar", "a.b", "axb",
				"start of it", "not start", "the end", "end of", "word", "wxd");
	}

	@Test
	void everythingMatchesWithoutAPattern() {
		var patterns = compile("abc/def");
		patterns.add(Optional.empty());
		check(patterns, "anything", "");
	}

	@Test
	void randomPatternsAgreeWithRunningThemAll() {
		Random random = new Random(1234);
		String[] pieces = {"abc", "de", "/", "_", ".*", ".", "\\.", "[a-d]", "x?", "y*", "z+", "json", "block", "(a|b)", "q{2}", "\\d"};
		for (int round = 0; round < 300; round++) {
			List<Optional<Pattern>> patterns = new ArrayList<>();
			for (int i = 0; i < 12; i++) {
				StringBuilder regex = new StringBuilder();
				for (int j = random.nextInt(4) + 1; j > 0; j--) {
					regex.append(pieces[random.nextInt(pieces.length)]);
				}
				try {
					patterns.add(Optional.of(Pattern.compile(regex.toString())));
				} catch (RuntimeException ignored) {
					// Not every combination is a valid expression.
				}
			}
			PathFilterIndex index = new PathFilterIndex(patterns);
			for (int i = 0; i < 60; i++) {
				StringBuilder path = new StringBuilder();
				for (int j = random.nextInt(8); j >= 0; j--) {
					path.append(pieces[random.nextInt(pieces.length)].replaceAll("[^a-z/._]", "x").replace("*", "").replace("?", ""));
				}
				String text = path.toString();
				assertEquals(naive(patterns, text), index.matches(text), text);
			}
		}
	}

	@Test
	void literalsAreFoundWhereTheyMustBe() {
		assertEquals(List.of("advancement/recipes/", "/arrow", "json"), RequiredLiterals.of(Pattern.compile("advancement/recipes/.*/arrow.json")));
		assertTrue(RequiredLiterals.of(Pattern.compile("a(b|c)def")).isEmpty());
		assertTrue(RequiredLiterals.of(Pattern.compile("(?i)stone")).isEmpty());
	}
}
