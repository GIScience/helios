package de.uni_hd.giscience.helios.core.scene;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;

public class RaySceneIntersection {
	public Primitive prim;
	public Vector3D point;
	public double incidenceAngle = 0;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(incidenceAngle);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((point == null) ? 0 : point.hashCode());
		result = prime * result + ((prim == null) ? 0 : prim.hashCode());
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
		RaySceneIntersection other = (RaySceneIntersection) obj;
		if (Double.doubleToLongBits(incidenceAngle) != Double.doubleToLongBits(other.incidenceAngle))
			return false;
		if (point == null) {
			if (other.point != null)
				return false;
		} else if (!point.equals(other.point))
			return false;
		if (prim == null) {
			if (other.prim != null)
				return false;
		} else if (!prim.equals(other.prim))
			return false;
		return true;
	}
}
