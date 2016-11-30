package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public class FiberArrayBeamDeflector extends AbstractBeamDeflector {

	int cfg_device_numFibers = 32;
	int state_currentFiber = 0;


	public FiberArrayBeamDeflector(double scanAngleMax_rad, int numFibers) {

		super(scanAngleMax_rad);
	
		cfg_device_numFibers = numFibers;
	}
	
	@Override
	public void applySettings(ScannerSettings settings) {
		super.applySettings(settings);

		currentBeamAngleInRad = -this.currentScanAngleInRad;
		setNumFibers(cfg_device_numFibers);
	}
	

	public void setNumFibers(int numFibers) {
		this.cfg_device_numFibers = numFibers;

		rotationAngleBetweenPulsesInRad = (this.currentScanAngleInRad * 2) / cfg_device_numFibers;
	}

	@Override
	public void doSimStep() {

		currentBeamAngleInRad = -this.currentScanAngleInRad + rotationAngleBetweenPulsesInRad * state_currentFiber;

		state_currentFiber++;
		if (state_currentFiber >= cfg_device_numFibers) {
			state_currentFiber = 0;
		}

		// Compute relative beam direction:
		this.orientation = new Rotation(Directions.right, currentBeamAngleInRad);
	}

	@Override
	public boolean hasLastPulseLeftDevice() {
		return true;
	}
}
