package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public class FiberArrayBeamDeflector extends AbstractBeamDeflector {

	int cfg_device_numFibers = 32;
	int state_currentFiber = 0;

	
	public FiberArrayBeamDeflector(double scanAngleMax_rad, double scanFreqMax_Hz, double scanFreqMin_Hz, int numFibers) {
			
		super(scanAngleMax_rad, scanFreqMax_Hz, scanFreqMin_Hz);
	
		cfg_device_numFibers = numFibers;
	}
	
	@Override
	public void applySettings(ScannerSettings settings) {
		super.applySettings(settings);

		state_currentBeamAngle_rad = -this.cfg_setting_scanAngle_rad;
		setNumFibers(cfg_device_numFibers);
	}
	

	public void setNumFibers(int numFibers) {
		this.cfg_device_numFibers = numFibers;

		cached_angleBetweenPulses_rad = (this.cfg_setting_scanAngle_rad * 2) / cfg_device_numFibers;
	}

	@Override
	public void doSimStep() {

		state_currentBeamAngle_rad = -this.cfg_setting_scanAngle_rad + cached_angleBetweenPulses_rad * state_currentFiber;

		state_currentFiber++;
		if (state_currentFiber >= cfg_device_numFibers) {
			state_currentFiber = 0;
		}

		// Compute relative beam direction:
		this.cached_emitterRelativeAttitude = new Rotation(Directions.RIGHT, state_currentBeamAngle_rad);
	}
}
