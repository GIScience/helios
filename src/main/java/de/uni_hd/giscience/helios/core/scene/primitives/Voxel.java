package de.uni_hd.giscience.helios.core.scene.primitives;

import java.util.ArrayList;

import javax.vecmath.Color4f;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Voxel extends Primitive {

	public Vertex v = new Vertex();

	public int numPoints = 0;

	public double r = 0;
	public double g = 0;
	public double b = 0;

	AABB bbox = null;

	public Color4f color;

	double halfSize;

	public Voxel(Vector3D center, double voxelSize) {
		v.pos = center;

		this.halfSize = voxelSize / 2;

		update();

	}

	@Override
	public AABB getAABB() {
		return bbox;
	}

	@Override
	public Vector3D getCentroid() {

		return v.pos;
	}

	@Override
	public double getIncidenceAngle_rad(Vector3D rayOrigin, Vector3D rayDir) {
		return 0;
	}

	@Override
	public double[] getRayIntersection(Vector3D rayOrigin, Vector3D rayDir) {
		return bbox.getRayIntersection(rayOrigin, rayDir);
	}

	@Override
	public ArrayList<Vertex> getVertices() {

		ArrayList<Vertex> result = new ArrayList<>();
		result.add(v);

		return result;
	}

	@Override
	public void update() {

		Vector3D hs = new Vector3D(halfSize, halfSize, halfSize);

		bbox = new AABB(v.pos.subtract(hs), v.pos.add(hs));
	}
}
