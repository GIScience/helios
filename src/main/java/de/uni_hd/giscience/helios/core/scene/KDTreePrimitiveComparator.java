package de.uni_hd.giscience.helios.core.scene;

import java.util.Comparator;

import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;

public class KDTreePrimitiveComparator implements Comparator<Primitive> {

	int axis = 0;

	public KDTreePrimitiveComparator(int axis) {
		this.axis = axis;
	}

	@Override
	public int compare(Primitive a, Primitive b) {

		double[] centroid_a = a.getCentroid().toArray();
		double[] centroid_b = b.getCentroid().toArray();

		if (centroid_a[axis] < centroid_b[axis]) {
			return -1;
		} else if (centroid_a[axis] > centroid_b[axis]) {
			return 1;
		}

		return 0;
	}
}