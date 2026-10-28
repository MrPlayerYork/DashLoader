package dev.notalpha.dashloader.misc.filter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Finds which of a set of strings occur in a text, in a single pass over the text (Aho-Corasick).
 */
final class LiteralSearch {
	private static final class Node {
		char[] keys = new char[0];
		Node[] children = new Node[0];
		Node fail;
		int[] found = new int[0];

		Node child(char c) {
			for (int i = 0; i < this.keys.length; i++) {
				if (this.keys[i] == c) {
					return this.children[i];
				}
			}
			return null;
		}

		Node add(char c) {
			Node existing = child(c);
			if (existing != null) {
				return existing;
			}
			this.keys = Arrays.copyOf(this.keys, this.keys.length + 1);
			this.children = Arrays.copyOf(this.children, this.children.length + 1);
			Node node = new Node();
			this.keys[this.keys.length - 1] = c;
			this.children[this.children.length - 1] = node;
			return node;
		}
	}

	private final Node root = new Node();

	/** The strings get the numbers 0, 1, 2, ... in the order they are given. */
	LiteralSearch(List<String> literals) {
		for (int id = 0; id < literals.size(); id++) {
			Node node = this.root;
			for (char c : literals.get(id).toCharArray()) {
				node = node.add(c);
			}
			node.found = Arrays.copyOf(node.found, node.found.length + 1);
			node.found[node.found.length - 1] = id;
		}

		ArrayDeque<Node> queue = new ArrayDeque<>();
		for (Node child : this.root.children) {
			child.fail = this.root;
			queue.add(child);
		}
		while (!queue.isEmpty()) {
			Node node = queue.poll();
			for (int i = 0; i < node.keys.length; i++) {
				Node child = node.children[i];
				Node fail = node.fail;
				while (fail != this.root && fail.child(node.keys[i]) == null) {
					fail = fail.fail;
				}
				Node target = fail.child(node.keys[i]);
				child.fail = target == null || target == child ? this.root : target;
				if (child.fail.found.length > 0) {
					List<Integer> all = new ArrayList<>();
					for (int id : child.found) {
						all.add(id);
					}
					for (int id : child.fail.found) {
						all.add(id);
					}
					child.found = all.stream().mapToInt(Integer::intValue).toArray();
				}
				queue.add(child);
			}
		}
	}

	/** @return the numbers of the strings which occur in the text. */
	BitSet search(String text) {
		BitSet present = new BitSet();
		Node node = this.root;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			Node next = node.child(c);
			while (next == null && node != this.root) {
				node = node.fail;
				next = node.child(c);
			}
			node = next == null ? this.root : next;
			for (int id : node.found) {
				present.set(id);
			}
		}
		return present;
	}
}
