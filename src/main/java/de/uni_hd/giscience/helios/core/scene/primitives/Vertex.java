package de.uni_hd.giscience.helios.core.scene.primitives;

import javax.vecmath.Color4f;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class Vertex {

	public Vector3D pos;
	public Vector3D normal;
	public Color4f color;
	public Vector2D texcoords;

	public Vertex copy() {
		Vertex v = new Vertex();
		v.pos = new Vector3D(pos.getX(), pos.getY(), pos.getZ());
		v.normal = new Vector3D(normal.getX(), normal.getY(),normal.getZ());
		v.color = new Color4f(color.x, color.y, color.z, color.w);
		v.texcoords = new Vector2D(texcoords.getX(), texcoords.getY());
		
		return v;
	}
	
	public double getX() {
		return this.pos.getX();
	}

	public double getY() {
		return this.pos.getY();
	}

	public double getZ() {
		return this.pos.getZ();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + ((normal == null) ? 0 : normal.hashCode());
		result = prime * result + ((pos == null) ? 0 : pos.hashCode());
		result = prime * result + ((texcoords == null) ? 0 : texcoords.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vertex other = (Vertex) obj;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		if (normal == null) {
			if (other.normal != null)
				return false;
		} else if (!normal.equals(other.normal))
			return false;
		if (pos == null) {
			if (other.pos != null)
				return false;
		} else if (!pos.equals(other.pos))
			return false;
		if (texcoords == null) {
			if (other.texcoords != null)
				return false;
		} else if (!texcoords.equals(other.texcoords))
			return false;
		return true;
	}

}
