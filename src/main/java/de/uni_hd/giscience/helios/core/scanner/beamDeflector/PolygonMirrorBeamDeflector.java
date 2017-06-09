package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import de.uni_hd.giscience.helios.Directions;

public class PolygonMirrorBeamDeflector extends AbstractBeamDeflector {

	protected double cfg_device_scanAngleEffective_rad = 0;
	protected double cfg_device_scanAngleEffectiveMax_rad = 0;

	
	public PolygonMirrorBeamDeflector(	 
										double scanFreqMax_Hz, 
										double scanFreqMin_Hz,
										double scanAngleMax_rad,
										double scanAngleEffectiveMax_rad) {
		
		super(scanAngleMax_rad, scanFreqMax_Hz, scanFreqMin_Hz);
		
		this.cfg_device_scanAngleEffectiveMax_rad = scanAngleEffectiveMax_rad;
		this.cfg_device_scanAngleEffective_rad = this.cfg_device_scanAngleEffectiveMax_rad;
	}


	@Override
	public void doSimStep() {

		// Update beam angle:
		state_currentBeamAngle_rad += cached_angleBetweenPulses_rad;

		if (state_currentBeamAngle_rad >= this.cfg_setting_scanAngle_rad) {
			state_currentBeamAngle_rad = -this.cfg_setting_scanAngle_rad;
		}

		// Rotate to current position:
		this.cached_emitterRelativeAttitude = new Rotation(Directions.RIGHT, state_currentBeamAngle_rad);
	}

	@Override
	public boolean lastPulseLeftDevice() {		
		return Math.abs(this.state_currentBeamAngle_rad) <= this.cfg_device_scanAngleEffective_rad;
	}
}
