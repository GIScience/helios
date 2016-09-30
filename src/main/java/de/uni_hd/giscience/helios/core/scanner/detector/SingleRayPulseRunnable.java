package de.uni_hd.giscience.helios.core.scanner.detector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.scene.RaySceneIntersection;
import de.uni_hd.giscience.helios.core.scene.Scene;

public class SingleRayPulseRunnable extends AbstractPulseRunnable {

	public SingleRayPulseRunnable(AbstractDetector detector, Vector3D absoluteBeamOrigin, Rotation absoluteBeamAttitude, int pulseNumber, Long gpstime) {
		super(detector, absoluteBeamOrigin, absoluteBeamAttitude, pulseNumber, gpstime);
	}

	@Override
	public void run() {

		Scene scene = detector.scanner.platform.scene;

		Vector3D beamDir = absoluteBeamAttitude.applyTo(forward);

		// Early abort if central axis of the beam does not intersect with the scene:
		double[] tMinMax = scene.getAABB().getRayIntersection(absoluteBeamOrigin, beamDir);

		if (tMinMax == null) {
			detector.scanner.setLastPulseWasHit(false);
			return;
		}

		RaySceneIntersection intersect = scene.getIntersection(absoluteBeamOrigin, beamDir, false);

		if (intersect == null || intersect.point == null) {
			detector.scanner.setLastPulseWasHit(false);
			return;
		}

		detector.scanner.setLastPulseWasHit(true);
		
		double distance = intersect.point.distance(absoluteBeamOrigin);
		double incidenceAngle = intersect.prim.getIncidenceAngle_rad(absoluteBeamOrigin, beamDir);

		//double intensity = calcIntensity(incidenceAngle, distance, intersect.prim.material.reflectance, 0);
		
		double intensity = Math.cos(incidenceAngle) * (1.0 / distance);

		capturePoint(absoluteBeamOrigin, beamDir, distance, intensity, 0, currentPulseNum, intersect.prim.part.mId);
	}

}
