package de.uni_hd.giscience.helios.core.scene;

import java.util.ArrayList;
import java.util.Comparator;

import de.uni_hd.giscience.helios.core.scene.primitives.AABB;
import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;

public class KDTreeNode {

	KDTreeNode left = null;
	KDTreeNode right = null;

	double splitPos = 0;
	int splitAxis = 0;

	static int stats_maxNumPrimsInLeaf = 0;
	static int stats_minNumPrimsInLeaf = Integer.MAX_VALUE;
	static int stats_maxDepthReached = 0;

	ArrayList<Primitive> primitives = null;

	public static KDTreeNode build(ArrayList<Primitive> primitives) {

		KDTreeNode root = buildRecursive(primitives, 0);

		System.out.println("\n");
		System.out.println("Max. # primitives in leaf: " + stats_maxNumPrimsInLeaf);
		System.out.println("Min. # primitives in leaf: " + stats_minNumPrimsInLeaf);
		System.out.println("Max. depth reached: : " + stats_maxDepthReached);

		return root;
	}

	private static KDTreeNode buildRecursive(ArrayList<Primitive> primitives, int depth) {

		int primsSize = primitives.size();

		if (primsSize == 0) {
			return null;
		}

		// Update maximum reached depth:
		if (depth > stats_maxDepthReached) {
			stats_maxDepthReached = depth;
		}

		KDTreeNode node = new KDTreeNode();

		// TODO 5: Implement surface area heuristics?
		int splitAxis = depth % 3;

		// Sort faces along split axis:
		// ATTENTION: Sorting must happen *BEFORE* splitPos is selecty using the median!!

		// Sort primitives along split axis:
		Comparator<Primitive> comparator = new KDTreePrimitiveComparator(splitAxis);
		java.util.Collections.sort(primitives, comparator);

		// Compute split position:
		double splitPos = primitives.get(primsSize / 2).getCentroid().toArray()[splitAxis];

		/*
		 * boolean spaltbar = false; boolean left = false; boolean right = false;
		 * 
		 * for (Primitive t : primitives) { AABB box = t.getAABB();
		 * 
		 * if (box.max.toArray()[splitAxis] <= splitPos) left = true; if (box.min.toArray()[splitAxis] > splitPos) right = true;
		 * 
		 * if (left && right) { spaltbar = true; break; } }
		 * 
		 * // If spaltbar, split up further: if (spaltbar) { // ########## BEGIN Fill children's primitive lists ##########
		 * 
		 * ArrayList<Primitive> sublist_left = new ArrayList<>(); ArrayList<Primitive> sublist_right = new ArrayList<>();
		 * 
		 * for (Primitive t : primitives) {
		 * 
		 * AABB box = t.getAABB();
		 * 
		 * if (box.min.toArray()[splitAxis] <= splitPos) { sublist_left.add(t); }
		 * 
		 * if (box.max.toArray()[splitAxis] > splitPos) { sublist_right.add(t); } }
		 * 
		 * node.splitPos = splitPos; node.splitAxis = splitAxis;
		 * 
		 * if (sublist_left.size() > 0) { node.left = buildRecursive(sublist_left, depth + 1); }
		 * 
		 * if (sublist_right.size() > 0) { node.right = buildRecursive(sublist_right, depth + 1); }
		 * 
		 * return node;
		 * 
		 * } else { // Otherwise, make this node a leaf: node.primitives = primitives; node.splitAxis = -1;
		 * 
		 * return node; }
		 */

		// ########## BEGIN Fill children's primitive lists ##########

		ArrayList<Primitive> sublist_left = new ArrayList<>();
		ArrayList<Primitive> sublist_right = new ArrayList<>();

		for (Primitive t : primitives) {

			AABB box = t.getAABB();

			if (box.min.toArray()[splitAxis] <= splitPos) {
				sublist_left.add(t);
			}

			if (box.max.toArray()[splitAxis] > splitPos) {
				sublist_right.add(t);
			}
		}

		if (sublist_left.size() != primsSize && sublist_right.size() != primsSize) {

			node.splitAxis = splitAxis;
			node.splitPos = splitPos;

			if (sublist_left.size() > 0) {
				node.left = buildRecursive(sublist_left, depth + 1);
			}

			if (sublist_right.size() > 0) {
				node.right = buildRecursive(sublist_right, depth + 1);
			}
		} else {
			// Otherwise, make this node a leaf:
			node.splitAxis = -1;
			node.primitives = primitives;
		}

		return node;

	}
}
