package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import de.uni_hd.giscience.helios.Directions;

public class PolygonMirrorBeamDeflector extends AbstractBeamDeflector {

	protected double cfg_device_scanAngleEffective_rad = 0;
	protected double cfg_device_scanAngleEffectiveMax_rad = 0;


	public PolygonMirrorBeamDeflector(double scanAngleMax_rad,
									  double scanAngleEffectiveMax_rad) {

		super(scanAngleMax_rad);
		
		this.cfg_device_scanAngleEffectiveMax_rad = scanAngleEffectiveMax_rad;
		this.cfg_device_scanAngleEffective_rad = this.cfg_device_scanAngleEffectiveMax_rad;
	}


	@Override
	public void doSimStep() {

		// Update beam angle:
		currentBeamAngleInRad += rotationAngleBetweenPulsesInRad;

		if (currentBeamAngleInRad >= this.currentScanAngleInRad) {
			currentBeamAngleInRad = -this.currentScanAngleInRad;
		}

		// Rotate to current position:
		this.orientation = new Rotation(Directions.right, currentBeamAngleInRad);
	}

	@Override
	public boolean hasLastPulseLeftDevice() {
		return Math.abs(this.currentBeamAngleInRad) <= this.cfg_device_scanAngleEffective_rad;
	}
}
