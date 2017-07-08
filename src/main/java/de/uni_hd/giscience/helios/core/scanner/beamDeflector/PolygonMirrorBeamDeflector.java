package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

/**
 * The polygon mirror beam deflector us a rotating mirror. In the real world
 * this mirror makes a 360 rotation with a fix frequency, called scan frequency.
 * The beam from the emitter is deflector on the mirror in the direction of
 * the current scan angle. In the configuration of a lidar system it is possible
 * to define the maximal deflection of the mirror.
 * This maximal deflection is the scan angle max.
 *
 * The scanner has to technical properties:
 *
 * - The supported maximal scan angle is the technical maximal usable scan angle.
 *   It describes the maximum value of the scanner windows to emit beams into the environment.
 *
 * - The scan frequency is the rotation speed of the mirror for 360 degree.
 *
 *
 * This class simulates the rotation of the mirror for deflection of the emitted
 * beam. The scan angle only runs over the range +- maximal deflection angle.
 * The rotation width of the mirror is defined by the scan frequency and
 * the pulse frequency of the emitter.
 *
 *       stepAngle = (360 degree / scan frequency) * pulse frequency
 *
 * The class will also create a last pulse left device state if the current scan
 * angle overruns the scan angle max.
 *
 *
 *         ^
 *  scan   |
 *  angle  |
 *         |
 *  scan   |
 *  angle  |
 *  max   +|         x          x          x
 *         |        x          x          x
 *         |       x          x          x
 *         |      x          x          x
 *         |     x          x          x
 *    0   +|    x          x          x
 *         |   x          x          x
 *         |  x          x          x
 *  scan   | x          x          x
 *  angle  |x          x          x
 *  -max  +x          x          x
 *         |
 *  	   +----------+----------+------------------------> doSimStep()s
 *         |          |          |
 *         +----------+----------+----------> HasLastPulseLeftDevice() == true
 *
 */
public class PolygonMirrorBeamDeflector implements IBeamDeflector {

	/**
	 * @param scanAngleSupportedMax_rad defines the maximum angle from center which the scanner supports to scan (scanner technical limitation)
	 */
	public PolygonMirrorBeamDeflector(
					double scanFreqMax_Hz,
					double scanFreqMin_Hz,
					double scanAngleSupportedMax_rad,
					double scanAngleEffectiveMax_rad) {

		this.scanAngleSupportedMaxInRad = scanAngleSupportedMax_rad;

		calculateMirrorAttitudeByCurrentBeamAngle();
	}

	private final double scanAngleSupportedMaxInRad;

	private double scanAngleMaxInRad = 0;
	private double angleBetweenSimulationStepsInRad = 0;


	private double currentScanAngleInRad = 0;
	private Rotation currentScanAngleAsAttitude;


	public void applySettings(ScannerSettings settings) {

		setScanAngleMaxInRad(settings.scanAngle_rad);

		setCurrentScanAngleInRad(-1 * scanAngleMaxInRad);
		calculateMirrorAttitudeByCurrentBeamAngle();

		calculateAngleBetweenSimulationSteps(settings);
	}

	public void doSimStep() {
		calculateNextBeamAngle();
		calculateMirrorAttitudeByCurrentBeamAngle();
	}

	public boolean HasLastPulseLeftDevice() {
		return currentScanAngleInRad != (-1 * scanAngleMaxInRad);
	}

	public Rotation getEmitterRelativeAttitude() {
		return this.currentScanAngleAsAttitude;
	}

	private void setScanAngleMaxInRad(double newScanAngleMaxInRad) {

		// \TODO(KoeMai) create warning for auto correction of scanAngle
		if (newScanAngleMaxInRad < 0) {
			newScanAngleMaxInRad = 0;
		} else if (newScanAngleMaxInRad > this.scanAngleSupportedMaxInRad) {
			newScanAngleMaxInRad = this.scanAngleSupportedMaxInRad;
		}

		this.scanAngleMaxInRad = newScanAngleMaxInRad;
	}

	private void setCurrentScanAngleInRad(double scanAngleInRad) {
		// restart rotation on overrun
		if (scanAngleInRad < this.scanAngleMaxInRad) {
			currentScanAngleInRad = scanAngleInRad;
		} else {
			currentScanAngleInRad = -this.scanAngleMaxInRad;
		}
	}

	double getCurrentScanAngleInRad() {
		return currentScanAngleInRad;
	}

	private void calculateAngleBetweenSimulationSteps(ScannerSettings settings) {
		double totalScanAngleRange = 2 * this.scanAngleMaxInRad;
		angleBetweenSimulationStepsInRad = totalScanAngleRange *
						((double) settings.scanFreq_Hz / (double) settings.scanFreq_Hz);
	}

	private void calculateMirrorAttitudeByCurrentBeamAngle() {
		currentScanAngleAsAttitude = new Rotation(Directions.right, currentScanAngleInRad);
	}

	private void calculateNextBeamAngle() {
		setCurrentScanAngleInRad(currentScanAngleInRad + angleBetweenSimulationStepsInRad);
	}
}
