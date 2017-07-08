package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

/**
 * The polygon mirror beam deflector is a rotating mirror. In the real world
 * this mirror makes a 360 rotation with a fix speed, called scan speed.
 * The beam from the emitter is deflector on the mirror in the direction of
 * the current scan angle. In the configuration of a lidar system it is possible
 * to define the maximal deflection of the mirror. This is called the scan angle
 * max.
 *
 * The scanner poly mirror has two main technical properties:
 *
 * - The supported maximal scan angle is the technical maximal
 *   usable scan angle. It describes the maximum supported scan angle.
 *   This limited because the mirror emit the beam over the mirror through a
 *   windows into the environment.
 *
 * - The minimal and maximal scan speed, the rotation speed of the mirror.
 *
 *
 * This class simulates the rotation of the mirror for deflection of the emitted
 * beam. The scan angle only runs over the range +- maximal deflection angle.
 * Scan angles larger then the supported scan angle are not simulated.
 *
 * The class will also create a last pulse left device state if the current scan
 * angle overruns the scan angle max.
 *
 * The figure below describes the relation between do simulation step and the
 * angle of the mirror. The current scan angle gradient of one simulation step
 * is defined by the simulation step duration and the scan speed.
 *
 *             ^
 *  scan       |
 *  angle      |
 *             |
 *  supported +|
 *  max        |
 *             |
 *  max       +|         x          x          x
 *             |        x          x          x
 *             |       x          x          x
 *             |      x          x          x
 *             |     x          x          x
 *    0       +|    x          x          x
 *             |   x          x          x
 *             |  x          x          x
 *             | x          x          x
 *             |x          x          x
 *  -max      +x          x          x
 *             |
 *  -supported+|
 *  max        |
 *  	       +----------+----------+------------------------> doSimStep()s
 *             |          |          |
 *             +----------+----------+----------> HasLastPulseLeftDevice() == true
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

		updateMirrorAttitudeByCurrentBeamAngle();
	}

	private final double scanAngleSupportedMaxInRad;

	private double scanAngleMaxInRad = 0;
	private double simStepDurationInSec = 0;
	private double scanSpeedInRadPerSec = 0;


	private double currentScanAngleInRad = 0;
	private Rotation currentScanAngleAsAttitude;

	private boolean isScanAngleOverrun = false;


	public void applySettings(ScannerSettings settings) {

		setScanAngleMaxInRad(settings.scanAngle_rad);

		setCurrentScanAngleInRad(-1 * scanAngleMaxInRad);
		updateMirrorAttitudeByCurrentBeamAngle();

		scanSpeedInRadPerSec = calculateScanSpeed(settings);
		simStepDurationInSec = 1.0 / (double)settings.pulseFreq_Hz;
	}

	private double calculateScanSpeed(ScannerSettings settings) {
		double totalScanAngleRange = 2 * this.scanAngleMaxInRad;
		return totalScanAngleRange / (double)settings.scanFreq_Hz;
	}

	public void doSimStep() {
		calculateNextBeamAngle(simStepDurationInSec);
		updateMirrorAttitudeByCurrentBeamAngle();
	}

	public boolean HasLastPulseLeftDevice() {
		return !isScanAngleOverrun;
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
			isScanAngleOverrun = false;
		} else {
			currentScanAngleInRad = -this.scanAngleMaxInRad;
			isScanAngleOverrun = true;
		}
	}

	private void updateMirrorAttitudeByCurrentBeamAngle() {
		currentScanAngleAsAttitude = new Rotation(Directions.right, currentScanAngleInRad);
	}

	private void calculateNextBeamAngle( double durationInSec) {
		double rotationAngleInRad = scanSpeedInRadPerSec * durationInSec;
		setCurrentScanAngleInRad(currentScanAngleInRad + rotationAngleInRad);
	}
}
