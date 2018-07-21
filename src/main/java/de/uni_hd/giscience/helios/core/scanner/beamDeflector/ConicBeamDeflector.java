package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public class ConicBeamDeflector extends AbstractBeamDeflector {

	public ConicBeamDeflector(double scanAngleMax_rad, double scanFreqMax_Hz, double scanFreqMin_Hz) {
		super(scanAngleMax_rad, scanFreqMax_Hz, scanFreqMin_Hz);
		// TODO Auto-generated constructor stub
	}


	// r1 is the rotation that creates the radius of the cone
	Rotation r1 = null;

	@Override
	public void applySettings(ScannerSettings settings) {

		super.applySettings(settings);
			
		cached_angleBetweenPulses_rad = (double) (this.cfg_device_scanFreqMax_Hz * Math.PI * 2) / settings.pulseFreq_Hz;

		r1 = new Rotation(new Vector3D(1, 0, 0), this.cfg_setting_scanAngle_rad);
	}
	

	@Override
	public void doSimStep() {

		// ####### BEGIN Update mirror angle ########
		state_currentBeamAngle_rad += cached_angleBetweenPulses_rad;

		if (state_currentBeamAngle_rad >= Math.PI * 2) {
			state_currentBeamAngle_rad = state_currentBeamAngle_rad % (Math.PI * 2);
		}
		// ####### END Update mirror angle ########

		// Rotate to current position on the cone circle:
		Rotation r2 = new Rotation(Directions.forward, state_currentBeamAngle_rad);

		this.cached_emitterRelativeAttitude = r2.applyTo(r1);
	}

}