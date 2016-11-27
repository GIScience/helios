package de.uni_hd.giscience.helios.core.scanner;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * This class represents the scanner head it is used to rotate
 * the beam deflector.
 * The simulation calculations on each doSimStep() call, the rotation
 * steps out of rotation speed (rad/s). This rotation is
 * provided as scanner head orientation around the
 * scanner head axis.
 *
 */
public class ScannerHead {

  /**
   * Defines the rotation axis of the scanner.
   */
  private final Vector3D rotationAxis;

  /**
   * Defines the maximal radial rotation per second of the scanner (rad/s).
   */
  private final double maximalRotationSpeedInRad;

  /**
   * Stores the speed of rotation (rad/s) for simulation.
   */
  private double rotationSpeedInRad = 0;

  /**
   * Defines the end of simulation. This value is used to signal end of simulation.
   */
  private double endOfRotationSimulationInRad = 0;

  /**
   * Stores the rotation angle for the current simulation step.
   */
  private double currentRotateAngleInRad = 0;

  /**
   * Stores the rotation matrix for the current simulation step.
   */
  private Rotation currentHeadOrientation = new Rotation(new Vector3D(0, 1, 0), 0);

  /**
   * Constructs the scanner head simulation part.
   *
   * @param headRotationAxis     Rotation axis of the scanner
   * @param maxHeadRotationSpeed Maximal supported rotation speed of the scanner (rad/s)
   */
  public ScannerHead(Vector3D headRotationAxis, double maxHeadRotationSpeed) {
    this.rotationAxis = headRotationAxis;
    this.maximalRotationSpeedInRad = maxHeadRotationSpeed;
  }

  /**
   * Updates Scanner head simulation properties from a scanner configuration.
   *
   * @param settings Scanner configuration
   */
  public void applySettings(ScannerSettings settings) {

    this.setRotatePerSecInRad(settings.headRotatePerSec_rad);
    this.setCurrentRotateAngleInRad(settings.headRotateStart_rad);

    // Set rotate stop angle:
    this.endOfRotationSimulationInRad = settings.headRotateStopInRad;
  }

  /**
   * Calculates the next rotation step for the scanner head.
   *
   * @param pulseFreqInHz rotation simulation step width in Hz (1/s)
   * @throws Exception If the pulse frequency is zero or negative
   */
  public void doSimStep(double pulseFreqInHz) throws Exception {

    if (pulseFreqInHz > 0) {
      setCurrentRotateAngleInRad(currentRotateAngleInRad + rotationSpeedInRad / pulseFreqInHz);
    } else {
      throw new Exception(
              String.format("Pulse frequency (Simulation step width ) has a illegal value (%1$,.2f <= 0).",
                      pulseFreqInHz));
    }
  }

  /**
   * Provides orientation of scanner head.
   *
   * @return scanner head orientation
   */
  public Rotation getHeadOrientation() {
    return this.currentHeadOrientation;
  }

  /**
   * Returns completeness information of the rotation simulation.
   *
   * @return Returns True if the rotation is completed, False when it is still running
   */
  public boolean rotateCompleted() {

    boolean result;

    if (rotationSpeedInRad < 0) {
      result = currentRotateAngleInRad <= endOfRotationSimulationInRad;
    } else {
      result = currentRotateAngleInRad >= endOfRotationSimulationInRad;
    }

    return result;
  }

  /**
   * Updates the current rotation angle of the scanner head.
   * This update will prepare the head orientation.
   *
   * @param angleInRad rotation angle of the scanner head in radial
   */
  private void setCurrentRotateAngleInRad(double angleInRad) {

    if (angleInRad == currentRotateAngleInRad) {
      return;
    }

    currentRotateAngleInRad = angleInRad;
    currentHeadOrientation = new Rotation(rotationAxis, currentRotateAngleInRad % (2 * Math.PI));
  }

  /**
   * Sets the rotation speed, this is used in the doSimStep()
   * for calculation of the next simulation step.
   *
   * @param rotateSpeed rotation speed in radial per second.
   *                    The value has to be smaller then maximal allowed rotation speed.
   *                    If the value is larger then the maximal allowed rotation speed is set.
   */
  private void setRotatePerSecInRad(double rotateSpeed) {

    // Limit head rotate speed to device maximum:
    if (Math.abs(rotateSpeed) > maximalRotationSpeedInRad) {
      rotateSpeed = Math.signum(rotateSpeed) * maximalRotationSpeedInRad;
    }

    this.rotationSpeedInRad = rotateSpeed;
  }
}
