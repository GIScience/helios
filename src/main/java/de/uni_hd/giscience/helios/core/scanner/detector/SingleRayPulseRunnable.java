package de.uni_hd.giscience.helios.core.scanner.detector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.LasSpecification;
import de.uni_hd.giscience.helios.core.scene.RaySceneIntersection;
import de.uni_hd.giscience.helios.core.scene.Scene;

public class SingleRayPulseRunnable extends AbstractPulseRunnable {

	public SingleRayPulseRunnable(AbstractDetector detector, Vector3D absoluteBeamOrigin, Rotation absoluteBeamAttitude, int pulseNumber, Long gpstime, int scanDirection) {
		super(detector, absoluteBeamOrigin, absoluteBeamAttitude, pulseNumber, gpstime, scanDirection);
	}

	@Override
	public void run() {

		Scene scene = detector.scanner.platform.scene;

		Vector3D beamDir = absoluteBeamAttitude.applyTo(Directions.forward);

		// Early abort if central axis of the beam does not intersect with the scene:
		double[] tMinMax = scene.getAABB().getRayIntersection(absoluteBeamOrigin, beamDir);

		if (tMinMax == null) {
			detector.scanner.setLastPulseWasHit(false);
			return;
		}

		RaySceneIntersection intersect = scene.getIntersection(absoluteBeamOrigin, beamDir, false);

		if (intersect == null || intersect.point == null || intersect.prim.material.classification == LasSpecification.WATER) {  // TODO: Deal with water do not just ignore it
			detector.scanner.setLastPulseWasHit(false);
			return;
		}

		detector.scanner.setLastPulseWasHit(true);
		
		double distance = intersect.point.distance(absoluteBeamOrigin);
		
		double incidenceAngle = intersect.prim.getIncidenceAngle_rad(absoluteBeamOrigin, beamDir);		
		
		double targetArea = detector.scanner.calcFootprintArea(distance); 
		
		double intensity = calcIntensity(incidenceAngle, distance, intersect.prim.material.reflectance, intersect.prim.material.specularity, targetArea);

		capturePoint(absoluteBeamOrigin, beamDir, distance, intensity, 0, 0, currentPulseNum, 0, intersect.prim.part.mId, intersect.prim.material.classification);
	}
}
