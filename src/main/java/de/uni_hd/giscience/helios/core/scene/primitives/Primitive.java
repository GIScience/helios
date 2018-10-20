package de.uni_hd.giscience.helios.core.scene.primitives;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.assetsloading.ScenePart;
import de.uni_hd.giscience.helios.core.scene.Material;

public abstract class Primitive implements Serializable {

	private static final long serialVersionUID = 74916691493048350L;

	public Material material = null;

	public ScenePart part = null;

	public abstract AABB getAABB();

	public abstract Vector3D getCentroid();

	public abstract double getIncidenceAngle_rad(Vector3D rayOrigin, Vector3D rayDir);

	public abstract double[] getRayIntersection(Vector3D rayOrigin, Vector3D rayDir);

	public abstract ArrayList<Vertex> getVertices();

	public void update() {
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((material == null) ? 0 : material.hashCode());
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
		Primitive other = (Primitive) obj;
		if (material == null) {
			if (other.material != null)
				return false;
		} else if (!material.equals(other.material))
			return false;
		return true;
	};
}
