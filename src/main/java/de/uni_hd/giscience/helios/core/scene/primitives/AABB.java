package de.uni_hd.giscience.helios.core.scene.primitives;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class AABB extends Primitive implements Serializable {

	private static final long serialVersionUID = 7460113617209305207L;
	public Vector3D min = null;
	public Vector3D max = null;

	public AABB(Vector3D min, Vector3D max) {

		this.min = min;
		this.max = max;
	}

	@Override
	public Vector3D getCentroid() {
		Vector3D size = getSize();

		return min.add(size.scalarMultiply(0.5));
	}

	public Vector3D getSize() {
		return max.subtract(min);
	}

	public static AABB getForPrimitives(ArrayList<Primitive> primitives) {

		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;

		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		double maxZ = Double.MIN_VALUE;

		for (Primitive p : primitives) {

			AABB box = p.getAABB();

			// Find minimum:
			if (box.min.getX() < minX) {
				minX = box.min.getX();
			}

			if (box.min.getY() < minY) {
				minY = box.min.getY();
			}

			if (box.min.getZ() < minZ) {
				minZ = box.min.getZ();
			}

			// Find maximum:
			if (box.max.getX() > maxX) {
				maxX = box.max.getX();
			}

			if (box.max.getY() > maxY) {
				maxY = box.max.getY();
			}

			if (box.max.getZ() > maxZ) {
				maxZ = box.max.getZ();
			}
		}

		Vector3D min = new Vector3D(minX, minY, minZ);
		Vector3D max = new Vector3D(maxX, maxY, maxZ);

		return new AABB(min, max);
	}

	public static AABB getForVertices(HashSet<Vertex> verts) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;

		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		double maxZ = Double.MIN_VALUE;

		for (Vertex v : verts) {
			// Find minimum:
			if (v.pos.getX() < minX) {
				minX = v.pos.getX();
			}

			if (v.pos.getY() < minY) {
				minY = v.pos.getY();
			}

			if (v.pos.getZ() < minZ) {
				minZ = v.pos.getZ();
			}

			// Find maximum:
			if (v.pos.getX() > maxX) {
				maxX = v.pos.getX();
			}

			if (v.pos.getY() > maxY) {
				maxY = v.pos.getY();
			}

			if (v.pos.getZ() > maxZ) {
				maxZ = v.pos.getZ();
			}

		}

		Vector3D min = new Vector3D(minX, minY, minZ);
		Vector3D max = new Vector3D(maxX, maxY, maxZ);

		AABB result = new AABB(min, max);
		return result;

	}

	public String toString() {

		String result = "";

		result += "Min: " + min.toString() + ", Max: " + max.toString();
		return result;
	}

	public double[] getRayIntersection(Vector3D orig, Vector3D dir) {

		// See http://www.scratchapixel.com/lessons/3d-basic-rendering/minimal-ray-tracer-rendering-simple-shapes/ray-box-intersection

		Vector3D invdir = new Vector3D(1.0f / dir.getX(), 1.0f / dir.getY(), 1.0f / dir.getZ());

		int[] sign = { 0, 0, 0 };

		sign[0] = invdir.getX() < 0 ? 1 : 0;
		sign[1] = invdir.getY() < 0 ? 1 : 0;
		sign[2] = invdir.getZ() < 0 ? 1 : 0;

		double tmin, tmax, tymin, tymax, tzmin, tzmax;

		Vector3D[] bounds = { min, max };

		tmin = (bounds[sign[0]].getX() - orig.getX()) * invdir.getX();
		tmax = (bounds[1 - sign[0]].getX() - orig.getX()) * invdir.getX();
		tymin = (bounds[sign[1]].getY() - orig.getY()) * invdir.getY();
		tymax = (bounds[1 - sign[1]].getY() - orig.getY()) * invdir.getY();

		if (tmin > tymax || tymin > tmax)
			return null;

		if (tymin > tmin) {
			tmin = tymin;
		}

		if (tymax < tmax) {
			tmax = tymax;
		}

		tzmin = (bounds[sign[2]].getZ() - orig.getZ()) * invdir.getZ();
		tzmax = (bounds[1 - sign[2]].getZ() - orig.getZ()) * invdir.getZ();

		if (tmin > tzmax || tzmin > tmax)
			return null;

		if (tzmin > tmin) {
			tmin = tzmin;
		}

		if (tzmax < tmax) {
			tmax = tzmax;
		}

		double[] result = { tmin, tmax };

		return result;
	}

	@Override
	public AABB getAABB() {
		return this;
	}

	@Override
	public ArrayList<Vertex> getVertices() {
		return null;
	}

	@Override
	public double getIncidenceAngle_rad(Vector3D rayOrigin, Vector3D rayDir) {
		// TODO 5: Implement this
		return 0;
	}
}
