package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public class PolygonMirrorBeamDeflector extends AbstractBeamDeflector {

	protected double cfg_device_scanAngleEffective_rad = 0;
	protected double cfg_device_scanAngleEffectiveMax_rad = 0;
	protected double cfg_device_scanAngleMin_rad = 0;
	protected double cfg_device_scanAngleMax_rad = 0;

	
	public PolygonMirrorBeamDeflector(	 
										double scanFreqMax_Hz, 
										double scanFreqMin_Hz,
										double scanAngleMax_rad,
										double scanAngleMin_rad,
										double scanAngleEffectiveMax_rad) {
		
		super(scanAngleMax_rad, scanFreqMax_Hz, scanFreqMin_Hz);
		
		this.cfg_device_scanAngleEffectiveMax_rad = scanAngleEffectiveMax_rad;
		this.cfg_device_scanAngleEffective_rad = this.cfg_device_scanAngleEffectiveMax_rad;
		this.cfg_device_scanAngleMin_rad = scanAngleMin_rad;
		this.cfg_device_scanAngleMax_rad = scanAngleMax_rad;
	}

	// Set the vertical scan angle settings within the scanner specifications
	protected void setScanAngleMinMax_rad(double scanAngleMin_rad, double scanAngleMax_rad) {
		
		if(scanAngleMin_rad == scanAngleMax_rad) {	// Not set 
			scanAngleMin_rad = cfg_device_scanAngleMin_rad;
			scanAngleMax_rad = cfg_device_scanAngleMax_rad;
		}
		else {			
			if (scanAngleMin_rad < cfg_device_scanAngleMin_rad) {
				scanAngleMin_rad = cfg_device_scanAngleMin_rad;
				System.out.println("Warning: Min vertical FOV has been modified to fit the scanner specifications.");
			} 
			
			if (scanAngleMax_rad > cfg_device_scanAngleMax_rad) {
				scanAngleMax_rad = cfg_device_scanAngleMin_rad;
				System.out.println("Warning: Max vertical FOV has been modified to fit the scanner specifications.");
			} 
		}	
		
		cfg_device_scanAngleMin_rad = scanAngleMin_rad;
		cfg_device_scanAngleMax_rad = scanAngleMax_rad;
		super.cfg_device_scanAngleRangeMax_rad = (cfg_device_scanAngleMax_rad - cfg_device_scanAngleMin_rad) / 2;
	}
	
	@Override
	public void applySettings(ScannerSettings settings) {

		setScanAngleMinMax_rad(settings.verticalAngleMin_rad, settings.verticalAngleMax_rad);
		super.applySettings(settings);	
		super.state_currentBeamAngle_rad = cfg_device_scanAngleMin_rad;
		System.out.println("Vertical angle settings: " +
				"Total Range: " + super.cfg_setting_scanAngleRange_rad * (180.0 / Math.PI) * 2 + "º " + 
				"Min: " + cfg_device_scanAngleMin_rad * (180.0 / Math.PI) + "º " + 
				"Max: " + cfg_device_scanAngleMax_rad * (180.0 / Math.PI) + "º ");
	}

	@Override
	public void doSimStep() {

		// Update beam angle:
		super.state_currentBeamAngle_rad += super.cached_angleBetweenPulses_rad;

		if (super.state_currentBeamAngle_rad >= cfg_device_scanAngleRangeMax_rad) {
			super.state_currentBeamAngle_rad = cfg_device_scanAngleMin_rad;
		}

		// Rotate to current position:
		super.cached_emitterRelativeAttitude = new Rotation(Directions.right, state_currentBeamAngle_rad);
	}

	@Override
	public boolean lastPulseLeftDevice() {		
		return Math.abs(super.state_currentBeamAngle_rad) <= cfg_device_scanAngleEffective_rad;
	}
}
