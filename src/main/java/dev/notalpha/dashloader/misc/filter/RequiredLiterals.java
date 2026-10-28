package dev.notalpha.dashloader.misc.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Finds text which every string matched by a regular expression has to contain. Only the simple cases are understood, for anything
 * else it says it found nothing, which is always safe.
 */
final class RequiredLiterals {
	private static final int MIN_LENGTH = 3;

	private RequiredLiterals() {
	}

	/**
	 * @return the literals, or an empty list if there is no way to tell (or none worth looking for).
	 */
	static List<String> of(Pattern pattern) {
		if (pattern.flags() != 0) {
			return List.of();
		}
		String regex = pattern.pattern();
		List<String> literals = new ArrayList<>();
		StringBuilder run = new StringBuilder();
		for (int i = 0; i < regex.length(); i++) {
			char c = regex.charAt(i);
			switch (c) {
				case '\\' -> {
					if (i + 1 >= regex.length()) {
						return List.of();
					}
					char next = regex.charAt(++i);
					if (next == 'Q' || next == 'E') {
						return List.of();
					}
					if (Character.isLetterOrDigit(next)) {
						// A character class, a boundary or a back reference, nothing literal.
						flush(run, literals);
					} else if (!append(run, next)) {
						return List.of();
					}
				}
				case '(', ')', '|' -> {
					return List.of();
				}
				case '[' -> {
					flush(run, literals);
					i = skipClass(regex, i);
					if (i < 0) {
						return List.of();
					}
				}
				case '{' -> {
					dropLast(run);
					flush(run, literals);
					i = regex.indexOf('}', i);
					if (i < 0) {
						return List.of();
					}
				}
				case '*', '?' -> {
					// The character before is optional.
					dropLast(run);
					flush(run, literals);
				}
				case '.', '+', '^', '$' -> flush(run, literals);
				default -> {
					if (!append(run, c)) {
						return List.of();
					}
				}
			}
		}
		flush(run, literals);
		return literals;
	}

	private static boolean append(StringBuilder run, char c) {
		if (c >= 128) {
			return false;
		}
		run.append(c);
		return true;
	}

	private static void dropLast(StringBuilder run) {
		if (!run.isEmpty()) {
			run.setLength(run.length() - 1);
		}
	}

	private static void flush(StringBuilder run, List<String> literals) {
		if (run.length() >= MIN_LENGTH) {
			String literal = run.toString();
			if (!literals.contains(literal)) {
				literals.add(literal);
			}
		}
		run.setLength(0);
	}

	/** @return the index of the closing bracket, or -1 if this is a class which is not understood (nested ones). */
	private static int skipClass(String regex, int open) {
		for (int i = open + 1; i < regex.length(); i++) {
			char c = regex.charAt(i);
			if (c == '\\') {
				i++;
			} else if (c == '[') {
				return -1;
			} else if (c == ']' && i > open + 1) {
				return i;
			}
		}
		return -1;
	}
}
