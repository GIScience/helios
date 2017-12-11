package de.uni_hd.giscience.helios.core.scene.primitives;

import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Color4f;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class Triangle extends Primitive {

	private static final long serialVersionUID = -870342667378612605L;
	public final Vertex[] verts = new Vertex[3];
	private Vector3D faceNormal = new Vector3D(0, 0, 0);

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
		return Math.PI - Vector3D.angle(faceNormal, rayDir);
	}
                
	// These naive methods are much faster than the built-in in Vector3D
	public double dotProductNaive(final Vector3D v1, Vector3D v3) {
		return v1.getX() * v3.getX() + v1.getY() * v3.getY() + v1.getZ() * v3.getZ();
    }
    
	public Vector3D crossProductNaive(Vector3D v1, Vector3D v2) {
		return new Vector3D(v1.getY() * v2.getZ() - v1.getZ()* v2.getY(),  
			v1.getZ() * v2.getX() - v1.getX()* v2.getZ(), 
			v1.getX() * v2.getY() - v1.getY()* v2.getX());
	}
               
	@Override
	/*
	 * Fast, Minimum Storage Ray/Triangle Intersection (Möller and Trumbore, 1997)
	 * See http://www.lighthouse3d.com/tutorials/maths/ray-triangle-intersection/
	 */
	public double[] getRayIntersection(Vector3D rayOrigin, Vector3D rayDir) {
                                   
        double eps = 0.00001;
		double[] result = new double[1];

		Vector3D e1 = this.verts[1].pos.subtract(this.verts[0].pos);
		Vector3D e2 = this.verts[2].pos.subtract(this.verts[0].pos);

		Vector3D h = crossProductNaive(rayDir, e2);
		
		double a = dotProductNaive(e1, h);

		if (a > -eps && a < eps) {
			result[0] = -1;
			return result;
		}

		double f = 1.0 / a;

		Vector3D s = rayOrigin.subtract(this.verts[0].pos);

		double u = f * dotProductNaive(s, h);

		if (u < 0.0 || u > 1.0) {
			result[0] = -1;
			return result;
		}

		Vector3D q = crossProductNaive(s, e1);
		
		double v = f * dotProductNaive(rayDir, q);

		if (v < 0.0 || u + v > 1.0) {
			result[0] = -1;
			return result;
		}

		double t = f * dotProductNaive(e2, q);

		if (t > eps) {
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
	
	@Override
	public String toString() {
		return getVertices().get(0).getX() + " " + getVertices().get(0).getY() + " " + getVertices().get(0).getZ() + "\n" +
			   getVertices().get(1).getX() + " " + getVertices().get(1).getY() + " " + getVertices().get(1).getZ() + "\n" +
			   getVertices().get(2).getX() + " " + getVertices().get(2).getY() + " " + getVertices().get(2).getZ();
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
				
	public double calcArea2D() {
		double det = verts[0].getX() * (verts[1].getY() - verts[2].getY())
				   + verts[1].getX() * (verts[2].getY() - verts[0].getY())
				   + verts[2].getX() * (verts[0].getY() - verts[1].getY());
		
		return 0.5 * Math.abs(det);
	}
	
	public double calcArea3D() {
		Vector3D ab = new Vector3D(verts[1].getX() - verts[0].getX(), verts[1].getY() - verts[0].getY(), verts[1].getZ() - verts[0].getZ());
		Vector3D ac = new Vector3D(verts[2].getX() - verts[0].getX(), verts[2].getY() - verts[0].getY(), verts[2].getZ() - verts[0].getZ());
		double cross = crossProductNaive(ab, ac).getNorm();
		return 0.5 * cross;				
	}			
	
	public double euclideanDistance2D(Vector3D src, Vector3D dst) {
		double diffX = 0, diffY = 0;
		diffX = (src.getX() - dst.getX()) * (src.getX() - dst.getX());
		diffY = (src.getY() - dst.getY()) * (src.getY() - dst.getY());
		double dist = Math.sqrt(diffX + diffY);
		//System.out.println(dist);
		return dist;
	}
	
	public boolean isInsideCircle(Sphere circle) {
		if(euclideanDistance2D(verts[0].pos, circle.center) > circle.radius &&
				euclideanDistance2D(verts[1].pos, circle.center) > circle.radius &&
				euclideanDistance2D(verts[2].pos, circle.center) > circle.radius) {
			return false;
		}
		return true;
	}
}
