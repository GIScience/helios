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
	protected void setScanAngleMinMax_rad(ScannerSettings settings) {
		
		if(Math.abs(settings.verticalAngleMin_rad - settings.verticalAngleMax_rad) < 0.01) {	// Not set 
			settings.verticalAngleMin_rad = -settings.scanAngle_rad;
			settings.verticalAngleMax_rad =  settings.scanAngle_rad;
		}
        else {
            settings.scanAngle_rad = (settings.verticalAngleMax_rad - settings.verticalAngleMin_rad) / 2.0;
        }
        
        if (settings.verticalAngleMin_rad < cfg_device_scanAngleMin_rad) {         
            System.out.println("Error: Min vertical angle smaller than the supported by the scanner.");
            System.exit(-1);
        } 
        if (settings.verticalAngleMax_rad > cfg_device_scanAngleMax_rad) {
            System.out.println("Error: Max vertical angle larger than the supported by the scanner.");
            System.exit(-1);
        } 
		
		cfg_device_scanAngleMin_rad = settings.verticalAngleMin_rad;
		cfg_device_scanAngleMax_rad = settings.verticalAngleMax_rad;
	}
	
	@Override
	public void applySettings(ScannerSettings settings) {

		setScanAngleMinMax_rad(settings);
		super.applySettings(settings);	
		super.state_currentBeamAngle_rad = cfg_device_scanAngleMin_rad;
        System.out.println("Vertical resolution: " + (float) (cached_angleBetweenPulses_rad * 180 / Math.PI));
		System.out.println("Vertical angle settings: " +
				"FOV: " + super.cfg_setting_scanAngle_rad * (180.0 / Math.PI) * 2 + "º " + 
				"Min: " + cfg_device_scanAngleMin_rad * (180.0 / Math.PI) + "º " + 
				"Max: " + cfg_device_scanAngleMax_rad * (180.0 / Math.PI) + "º ");
	}

	@Override
	public void doSimStep() {

		// Update beam angle:
		super.state_currentBeamAngle_rad += super.cached_angleBetweenPulses_rad;

		if (super.state_currentBeamAngle_rad >= cfg_device_scanAngleMax_rad) {
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
