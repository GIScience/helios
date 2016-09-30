package de.uni_hd.giscience.helios.core.scene.primitives;

import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Color4f;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Triangle extends Primitive {

	public final Vertex[] verts = new Vertex[3];

	Vector3D faceNormal = new Vector3D(0, 0, 0);

	public Triangle(Vertex v0, Vertex v1, Vertex v2) {

		verts[0] = v0;
		verts[1] = v1;
		verts[2] = v2;

		update();
	}

	@Override
	public AABB getAABB() {
		double minX = Math.min(Math.min(verts[0].getX(), verts[1].getX()), verts[2].getX());
		double minY = Math.min(Math.min(verts[0].getY(), verts[1].getY()), verts[2].getY());
		double minZ = Math.min(Math.min(verts[0].getZ(), verts[1].getZ()), verts[2].getZ());

		Vector3D min = new Vector3D(minX, minY, minZ);

		double maxX = Math.max(Math.max(verts[0].getX(), verts[1].getX()), verts[2].getX());
		double maxY = Math.max(Math.max(verts[0].getY(), verts[1].getY()), verts[2].getY());
		double maxZ = Math.max(Math.max(verts[0].getZ(), verts[1].getZ()), verts[2].getZ());

		Vector3D max = new Vector3D(maxX, maxY, maxZ);

		return new AABB(min, max);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((faceNormal == null) ? 0 : faceNormal.hashCode());
		result = prime * result + Arrays.hashCode(verts);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Triangle other = (Triangle) obj;
		if (faceNormal == null) {
			if (other.faceNormal != null)
				return false;
		} else if (!faceNormal.equals(other.faceNormal))
			return false;
		if (!Arrays.equals(verts, other.verts))
			return false;
		return true;
	}

	@Override
	public Vector3D getCentroid() {

		return (verts[0].pos.add(verts[1].pos).add(verts[2].pos)).scalarMultiply(1.0 / 3);
	}

	public Vector3D getFaceNormal() {

		if (this.faceNormal == null) {
			this.update();
		}

		return this.faceNormal;
	}

	@Override
	public double getIncidenceAngle_rad(Vector3D rayOrigin, Vector3D rayDir) {
		return (Math.PI / 2) - Math.acos(rayDir.dotProduct(faceNormal));
	}

	@Override
	public double[] getRayIntersection(Vector3D rayOrigin, Vector3D rayDir) {

		// See http://www.lighthouse3d.com/tutorials/maths/ray-triangle-intersection/
		double[] result = new double[1];

		Vector3D e1 = this.verts[1].pos.subtract(this.verts[0].pos);
		Vector3D e2 = this.verts[2].pos.subtract(this.verts[0].pos);

		Vector3D h = rayDir.crossProduct(e2);

		double a = e1.dotProduct(h);

		if (a > -0.00001 && a < 0.00001) {
			result[0] = -1;
			return result;
		}

		double f = 1.0 / a;

		Vector3D s = rayOrigin.subtract(this.verts[0].pos);

		double u = f * s.dotProduct(h);

		if (u < 0.0 || u > 1.0) {
			result[0] = -1;
			return result;
		}

		Vector3D q = s.crossProduct(e1);

		double v = f * rayDir.dotProduct(q);

		if (v < 0.0 || u + v > 1.0) {
			result[0] = -1;
			return result;
		}

		double t = f * e2.dotProduct(q);

		if (t > 0.00001) {
			result[0] = t;
			return result;
		}

		result[0] = -1;
		return result;
	}

	@Override
	public ArrayList<Vertex> getVertices() {
		ArrayList<Vertex> result = new ArrayList<>();

		result.add(verts[0]);
		result.add(verts[1]);
		result.add(verts[2]);

		return result;
	}

	@Override
	public void update() {

		Vector3D normal_unnormalized = (verts[1].pos.subtract(verts[0].pos)).crossProduct(verts[2].pos.subtract(verts[0].pos));

		if (normal_unnormalized.getNorm() > 0) {
			this.faceNormal = normal_unnormalized.normalize();
		}
	}

	public void setAllVertexColors(Color4f color) {
		verts[0].color = color;
		verts[1].color = color;
		verts[2].color = color;
	}

	public void setAllVertexNormalsFromFace() {
		verts[0].normal = faceNormal;
		verts[1].normal = faceNormal;
		verts[2].normal = faceNormal;
	}
}
