package de.uni_hd.giscience.helios.surveyplanning;

import java.util.HashSet;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class ScanPosition {

	Vector3D pos = new Vector3D(0, 0, 0);

	HashSet<Vector3D> visiblePoints = new HashSet<>();

	HashSet<Vector3D> exclusivePoints = new HashSet<>();

	public double avgIncidenceAngle_rad = 0;

	public HashSet<Vector3D> getVisiblePoints() {
		return visiblePoints;
	}

	public int numVisiblePoints() {
		return visiblePoints.size();
	}

	public int numExclusivePoints() {
		return exclusivePoints.size();
	}

	public float getCoverage(HashSet<Vector3D> testPoints) {
		return ((float) getVisiblePoints().size()) / testPoints.size();
	}

	public boolean isRedundant() {
		return (this.exclusivePoints.size() == 0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pos == null) ? 0 : pos.hashCode());
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
		ScanPosition other = (ScanPosition) obj;
		if (pos == null) {
			if (other.pos != null)
				return false;
		} else if (!pos.equals(other.pos))
			return false;
		return true;
	}
}
