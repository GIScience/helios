
package de.uni_hd.giscience.helios.core.platform;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class LinearPathPlatform extends MovingPlatform {

	
	
	@Override
	public void doSimStep(int simFrequency_hz) {

		super.doSimStep(simFrequency_hz);

		if (getVectorToTarget().getNorm() > 0 && cfg_settings_movePerSec_m > 0) {

			// Set Velocity:
			double speed = cfg_settings_movePerSec_m / simFrequency_hz;

			this.setVelocity(getVectorToTarget().normalize().scalarMultiply(speed));
		}
	}

	@Override
	public void setDestination(Vector3D dest) {
		super.setDestination(dest);

		initLegManual();
		
	}
}
