package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

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
 * usable scan angle. It describes the maximum supported scan angle.
 * This limited because the mirror emit the beam over the mirror through a
 * windows into the environment.
 *
 * - The minimal and maximal scan speed, the rotation speed of the mirror.
 *
 *
 * This class simulates rotation of the mirror for deflection for emitted
 * beams. The scan angle only runs over the range +- maximal deflection angle.
 * Scan angles larger then the supported scan angle are not simulated.
 *
 * The figure below describes the relation between do simulation step and the
 * angle of the mirror. The current scan angle gradient of one simulation step
 * is defined by the simulation step duration and the scan speed.
 *
 * ^
 * scan       |
 * angle      |
 *            |
 * supported +|
 * max        |
 *            |
 * max       +|         x          x          x
 *            |        x          x          x
 *            |       x          x          x
 *            |      x          x          x
 *            |     x          x          x
 * 0         +|    x          x          x
 *            |   x          x          x
 *            |  x          x          x
 *            | x          x          x
 *            |x          x          x
 * -max      +x          x          x
 *            |
 * -supported+|
 * max        |
 * +----------+----------+------------------------> doSimStep()s
 */
public class PolygonMirrorBeamDeflector implements IBeamDeflector {

  /**
   * \todo(KoeMai) Refactor to scanSpeed, translation from scan frequency and
   * pulse frequency can by done by scanner.
   * \todo(KoeMai) Remove Frequency and effective max
   *
   * @param scanAngleSupportedMaxInRad defines the maximum angle
   *                                   from center which the scanner supports
   *                                   to scan (scanner technical limitation)
   */
  public PolygonMirrorBeamDeflector(
          double scanFreqMaxInHz,
          double scanFreqMinInHz,
          double scanAngleSupportedMaxInRad,
          double scanAngleEffectiveMaxinRad) {

    this.scanAngleSupportedMaxInRad = scanAngleSupportedMaxInRad;

    updateMirrorAttitudeByCurrentBeamAngle();
  }

  private final double scanAngleSupportedMaxInRad;

  private double scanAngleMaxInRad = 0;
  private double simStepDurationInSec = 0;
  private double scanSpeedInRadPerSec = 0;

  private double currentScanAngleInRad = 0;
  private Rotation currentScanAngleAsAttitude;


  /**
   * Updates simulation settings for the mirror simulation.
   *
   * @param settings scanner settings for simulation
   */
  public void applySettings(ScannerSettings settings) {

    setScanAngleMaxInRad(settings.scanAngle_rad);

    setCurrentScanAngleInRad(-1 * scanAngleMaxInRad);
    updateMirrorAttitudeByCurrentBeamAngle();

    scanSpeedInRadPerSec = calculateScanSpeed(settings);
    simStepDurationInSec = 1.0 / (double) settings.pulseFreq_Hz;
  }

  /**
   * Calculates scan speed out of angle and scan frequency.
   *
   * @param settings Scanner configuration with scan angle
   * @return returns scan speed
   */
  private double calculateScanSpeed(ScannerSettings settings) {
    double totalScanAngleRange = 2 * this.scanAngleMaxInRad;
    return totalScanAngleRange * (double) settings.scanFreq_Hz;
  }

  /**
   * Simulates the next mirror rotation position in simulation.
   * \todo(KoeMai) doSimStep gets duration. THe duration can be calculated in scanner)
   */
  public void doSimStep() {
    calculateNextBeamAngle(simStepDurationInSec);
    updateMirrorAttitudeByCurrentBeamAngle();
  }

  /**
   * Provides completeness of motion position.
   *
   * The polygon mirror beam deflector is finished with simulation on each
   * motion position. The mirror angle is independent of the motion position.
   * The emitted beams will have a position change.
   * For this kind of mirror are no multiply simulation steps needed.
   *
   * @return Always true
   */
  public boolean HasLastPulseLeftDevice() {
    return true;
  }

  /**
   * Mirror attitude of the last doSimStep().
   *
   * @return Represents the present mirror angle
   */
  public Rotation getEmitterRelativeAttitude() {
    return this.currentScanAngleAsAttitude;
  }

  /**
   * Sets the maximal expected scan angle for simulation.
   *
   * @param newScanAngleMaxInRad angle of simulation end
   */
  private void setScanAngleMaxInRad(double newScanAngleMaxInRad) {

    // \TODO(KoeMai) create warning for auto correction of scanAngle
    if (newScanAngleMaxInRad < 0) {
      newScanAngleMaxInRad = 0;
    } else if (newScanAngleMaxInRad > this.scanAngleSupportedMaxInRad) {
      newScanAngleMaxInRad = this.scanAngleSupportedMaxInRad;
    }

    this.scanAngleMaxInRad = newScanAngleMaxInRad;
  }

  /**
   * Sets the new present can angle.
   *
   * This function assign and corrects the overrun of the can angle
   *
   * @param scanAngleInRad present scan angle in rad
   */
  private void setCurrentScanAngleInRad(double scanAngleInRad) {
    // restart rotation on overrun
    boolean isScanAngleOverrun = scanAngleInRad >= this.scanAngleMaxInRad;

    if (isScanAngleOverrun) {
      double angleOfOverrunInRad = scanAngleInRad % this.scanAngleMaxInRad;
      currentScanAngleInRad = -this.scanAngleMaxInRad + angleOfOverrunInRad;
    } else {
      currentScanAngleInRad = scanAngleInRad;
    }
  }

  /**
   * Update mirror attitude out of the present scan angle.
   */
  private void updateMirrorAttitudeByCurrentBeamAngle() {
    currentScanAngleAsAttitude = new Rotation(Directions.right, currentScanAngleInRad);
  }

  /**
   * Calculates the next present scan angle for a given duration.
   *
   * @param durationInSec duration of mirror rotation of the single calculation step.
   */
  private void calculateNextBeamAngle(double durationInSec) {
    double rotationAngleInRad = scanSpeedInRadPerSec * durationInSec;
    setCurrentScanAngleInRad(currentScanAngleInRad + rotationAngleInRad);
  }
}
