package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public class ConicBeamDeflector extends AbstractBeamDeflector {

	double cfg_device_scanFreqMax_Hz = 0;

	public ConicBeamDeflector(double scanAngleMax_rad, double scanFreqMax_Hz) {
		super(scanAngleMax_rad);

		cfg_device_scanFreqMax_Hz = scanFreqMax_Hz;
	}


	// r1 is the rotation that creates the radius of the cone
	Rotation r1 = null;

	@Override
	public void applySettings(ScannerSettings settings) {

		super.applySettings(settings);

		rotationAngleBetweenPulsesInRad = (double) (this.cfg_device_scanFreqMax_Hz * Math.PI * 2) / settings.pulseFreq_Hz;

		r1 = new Rotation(new Vector3D(1, 0, 0), this.currentScanAngleInRad);
	}
	

	@Override
	public void doSimStep() {

		// ####### BEGIN Update mirror angle ########
		currentBeamAngleInRad += rotationAngleBetweenPulsesInRad;

		if (currentBeamAngleInRad >= Math.PI * 2) {
			currentBeamAngleInRad = currentBeamAngleInRad % (Math.PI * 2);
		}
		// ####### END Update mirror angle ########

		// Rotate to current position on the cone circle:
		Rotation r2 = new Rotation(Directions.forward, currentBeamAngleInRad);

		this.orientation = r2.applyTo(r1);
	}

	@Override
	public boolean hasLastPulseLeftDevice() {
		return true;
	}

}