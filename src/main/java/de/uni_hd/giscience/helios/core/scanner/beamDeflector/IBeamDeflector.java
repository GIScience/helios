package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

public interface IBeamDeflector {

	void applySettings(ScannerSettings settings);

	Rotation getEmitterRelativeAttitude();

	void doSimStep();


	boolean HasLastPulseLeftDevice();
}
