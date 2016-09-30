// TODO 4: Merge search_recursive() and search_all_recursive?

package de.uni_hd.giscience.helios.core.scene;

import java.util.TreeMap;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;

public class KdTreeRaycaster {

	double epsilon = 0.0001;

	boolean groundOnly = false;

	Vector3D rayDir;
	Vector3D rayOrigin;

	double[] rayDirArray;
	double[] rayOriginArray;

	KDTreeNode root = null;

	double closestHitDistance = Double.MAX_VALUE;

	TreeMap<Double, Primitive> collectedPoints = new TreeMap<Double, Primitive>();

	public KdTreeRaycaster(KDTreeNode root) {
		this.root = root;
	}

	public TreeMap<Double, Primitive> searchAll(Vector3D rayOrigin, Vector3D rayDir, double tmin, double tmax, boolean groundOnly) {

		this.rayDirArray = rayDir.toArray();
		this.rayOriginArray = rayOrigin.toArray();

		this.rayOrigin = rayOrigin;
		this.rayDir = rayDir;

		this.groundOnly = groundOnly;

		this.collectedPoints.clear();
		this.closestHitDistance = Double.MAX_VALUE;

		this.searchAll_recursive(this.root, tmin, tmax);

		return collectedPoints;
	}

	public RaySceneIntersection search(Vector3D rayOrigin, Vector3D rayDir, double tmin, double tmax, boolean groundOnly) {

		this.rayDirArray = rayDir.toArray();
		this.rayOriginArray = rayOrigin.toArray();

		this.rayOrigin = rayOrigin;
		this.rayDir = rayDir;

		this.groundOnly = groundOnly;

		this.closestHitDistance = Double.MAX_VALUE;

		Primitive prim = this.search_recursive(this.root, tmin, tmax);

		// System.out.println("complete!");
		if (prim == null) {
			// System.out.println("KDtree Miss");
			return null;
		}

		RaySceneIntersection result = new RaySceneIntersection();
		result.prim = prim;
		result.point = rayOrigin.add(rayDir.scalarMultiply(closestHitDistance));

		return result;

	}

	void searchAll_recursive(KDTreeNode node, double tmin, double tmax) {

		// ######### BEGIN If node is a leaf, perform ray-primitive intersection on all primitives in the leaf's bucket ###########
		if (node.splitAxis == -1) {

			for (Primitive prim : node.primitives) {

				double[] tMinMax = prim.getRayIntersection(rayOrigin, rayDir);

				if (tMinMax == null) {
					continue;
				}

				double newDistance = tMinMax[0];

				// NOTE:
				// Checking for tmin <= newDistance <= tmax here is REQUIRED to prevent the following scenario from producing wrong results:

				// Imagine a primitive extending across multiple partitions (i.e. kdtree leaves). Now, if the tree traversal algorithm
				// arrives at a leaf and checks for ray-primitive-intersections *without* the range check, it might detect an
				// intersection with a primitive that intersects with the partition, but the ray-primitive intersection is *outside*
				// of the partition. Traversal would stop and the intersection would be returned without checking if there are
				// *other* intersections (in other leaves, with other primitives) that are *closer* to the ray origin. If this was
				// the case, the returned intersection would be wrong.

				if (newDistance > 0 && (newDistance >= tmin - epsilon && newDistance <= tmax + epsilon)) {
					if (!groundOnly || (prim.material != null && prim.material.isGround)) {
						collectedPoints.put(newDistance, prim);
					}
				}
			}
		}
		// ######### END If node is a leaf, perform ray-primitive intersection on all primitives in the leaf's bucket ###########

		// ######### BEGIN If node is not a leaf, figure out which child node(s) to traverse next, in which order #############
		else {

			int a = node.splitAxis;

			double thit = Double.POSITIVE_INFINITY;

			KDTreeNode first = null, second = null;

			// ############ BEGIN Check ray direction to figure out thorugh which sides the ray passes in which order ###########

			// Case 1: Ray goes in positive direction - it passes through the left side first, then through the right:
			if (rayDirArray[a] > 0) {
				first = node.left;
				second = node.right;

				thit = (node.splitPos - this.rayOriginArray[a]) / rayDirArray[a];
			}
			// Case 2: Ray goes in negative direction - it passes through the right side first, then through the left:
			else if (rayDirArray[a] < 0) {
				first = node.right;
				second = node.left;

				thit = (node.splitPos - this.rayOriginArray[a]) / rayDirArray[a];
			}
			// Case 3: Ray goes parallel to the split plane - it passes through only one side, depending on it's origin:
			else {
				first = (rayOriginArray[a] < node.splitPos) ? node.left : node.right;
				second = (rayOriginArray[a] < node.splitPos) ? node.right : node.left;
			}
			// ############ END Check ray direction to figure out thorugh which sides the ray passes in which order ###########

			// ########### BEGIN Check where the ray crosses the split plane to figure out which sides we need to stop into at all ###########

			// thit >= tmax means that the ray crosses the split plane *after it has already left the volume*.
			// In this case, it never enters the second half.
			if (thit >= tmax) {
				searchAll_recursive(first, tmin, tmax);
			}

			// thit <= tmin means that the ray crosses the split plane *before it enters the volume*.
			// In this case, it never enters the first half:
			else if (thit <= tmin) {
				searchAll_recursive(second, tmin, tmax);
			}

			// Otherwise, the ray crosses the split plane within the volume.
			// This means that it passes through both sides:
			else {
				searchAll_recursive(first, tmin, thit);
				searchAll_recursive(second, thit, tmax);
			}
			// ########### END Check where the ray crosses the split plane to figure out which sides we need to stop into at all ###########
		}
		// ######### END If node is not a leaf, figure out which child node(s) to traverse next, in which order #############
	}

	Primitive search_recursive(KDTreeNode node, double tmin, double tmax) {

		Primitive hitPrim = null;

		// ######### BEGIN If node is a leaf, perform ray-primitive intersection on all primitives in the leaf's bucket ###########
		if (node.splitAxis == -1) {

			for (Primitive prim : node.primitives) {

				double[] tMinMax = prim.getRayIntersection(rayOrigin, rayDir);

				if (tMinMax == null) {
					continue;
				}

				double newDistance = tMinMax[0];

				// NOTE:
				// Checking for tmin <= newDistance <= tmax here is REQUIRED to prevent the following scenario from producing wrong results:

				// Imagine a primitive extending across multiple partitions (i.e. kdtree leaves). Now, if the tree traversal algorithm
				// arrives at a leaf and checks for ray-primitive-intersections *without* the range check, it might detect an
				// intersection with a primitive that intersects with the partition, but the ray-primitive intersection is *outside*
				// of the partition. Traversal would stop and the intersection would be returned without checking if there are
				// *other* intersections (in other leaves, with other primitives) that are *closer* to the ray origin. If this was
				// the case, the returned intersection would be wrong.

				if ((newDistance > 0 && newDistance < closestHitDistance) && (newDistance >= tmin - epsilon && newDistance <= tmax + epsilon)) {

					if (!groundOnly || (prim.material != null && prim.material.isGround)) {
						closestHitDistance = newDistance;
						hitPrim = prim;
					}
				}
			}
		}
		// ######### END If node is a leaf, perform ray-primitive intersection on all primitives in the leaf's bucket ###########

		// ######### BEGIN If node is not a leaf, figure out which child node(s) to traverse next, in which order #############
		else {

			int a = node.splitAxis;

			double thit = Double.POSITIVE_INFINITY;

			KDTreeNode first = null, second = null;

			// ############ BEGIN Check ray direction to figure out thorugh which sides the ray passes in which order ###########

			// Case 1: Ray goes in positive direction - it passes through the left side first, then through the right:
			if (rayDirArray[a] > 0) {
				first = node.left;
				second = node.right;

				thit = (node.splitPos - this.rayOriginArray[a]) / rayDirArray[a];
			}
			// Case 2: Ray goes in negative direction - it passes through the right side first, then through the left:
			else if (rayDirArray[a] < 0) {
				first = node.right;
				second = node.left;

				thit = (node.splitPos - this.rayOriginArray[a]) / rayDirArray[a];
			}
			// Case 3: Ray goes parallel to the split plane - it passes through only one side, depending on it's origin:
			else {
				first = (rayOriginArray[a] < node.splitPos) ? node.left : node.right;
				second = (rayOriginArray[a] < node.splitPos) ? node.right : node.left;
			}
			// ############ END Check ray direction to figure out thorugh which sides the ray passes in which order ###########

			// ########### BEGIN Check where the ray crosses the split plane to figure out which sides we need to stop into at all ###########

			// thit >= tmax means that the ray crosses the split plane *after it has already left the volume*.
			// In this case, it never enters the second half.
			if (thit >= tmax) {
				hitPrim = search_recursive(first, tmin, tmax);
			}

			// thit <= tmin means that the ray crosses the split plane *before it enters the volume*.
			// In this case, it never enters the first half:
			else if (thit <= tmin) {
				hitPrim = search_recursive(second, tmin, tmax);
			}

			// Otherwise, the ray crosses the split plane within the volume.
			// This means that it passes through both sides:
			else {
				hitPrim = search_recursive(first, tmin, thit);

				if (hitPrim == null) {
					hitPrim = search_recursive(second, thit, tmax);
				}
			}

			// ########### END Check where the ray crosses the split plane to figure out which sides we need to stop into at all ###########
		}
		// ######### END If node is not a leaf, figure out which child node(s) to traverse next, in which order #############

		return hitPrim;
	}
}
