package de.uni_hd.giscience.helios.core.scanner.detector;

import java.util.concurrent.ExecutorService;

import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.scanner.Scanner;

public class SingleRayPulseDetector extends AbstractDetector {

	public SingleRayPulseDetector(Scanner scanner, double accuracy_m, double range_min) {
		super(scanner, accuracy_m, range_min);
		// TODO Auto-generated constructor stub
	}

	// TODO 3: Perhaps return PulseRunnable and let the Scanner class submit it, instead of submitting it here?

	@Override
	public void simulatePulse(ExecutorService execService, Vector3D absoluteBeamOrigin, Rotation absoluteBeamAttitude, int currentPulseNumber, long currentGpsTime) {

		AbstractPulseRunnable worker = new SingleRayPulseRunnable(this, absoluteBeamOrigin, absoluteBeamAttitude, currentPulseNumber, currentGpsTime);

		// Submit pulse runnable to worker threads:
		execService.execute(worker);
	}

	@Override
	public void applySettings(ScannerSettings settings) {
	}
}
