package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

/**
 * The beam deflector is used to calculate the direction of
 * emitting the pulse. There are different deflection methods.
 * This class is the interface for the different defection method models.
 */
public abstract class AbstractBeamDeflector {

  /**
   * Defines the maximal allowed deflection angle.
   */
  private final double maximalDeflectionAngleInRad;

  /**
   * \todo unclear information.
   */
  private double scanFreqInHz = 0;

  /**
   * Stores the current scan radial angle of the deflector.
   * \todo what is the difference between scan angle and beam angle?
   */
  private double currentScanAngleInRad = 0;

  // Stat variables:
  protected double currentBeamAngleInRad = 0;

  protected double rotationAngleBetweenPulsesInRad;

  /**
   * Stores the current orientation of the emitter.
   */
  Rotation orientation = new Rotation(new Vector3D(1, 0, 0), 0);

  /**
   * Constructs the interface properties of the beam deflector.
   *
   * @param deflectionAngleMaxInRad maximal allowed beam deflections angle
   */
  public AbstractBeamDeflector(double deflectionAngleMaxInRad) {
    this.maximalDeflectionAngleInRad = deflectionAngleMaxInRad;
  }

  /**
   * Loads the beam deflector settings from scanner configuration.
   *
   * @param settings Scanner configuration of interest
   */
  public void applySettings(ScannerSettings settings) {

    setScanAngleInRad(settings.scanAngle_rad);
    setScanFreq_Hz(settings.scanFreq_Hz);

    currentBeamAngleInRad = 0;
    rotationAngleBetweenPulsesInRad =
            (this.scanFreqInHz * this.currentScanAngleInRad * 2)
                    / settings.pulseFreq_Hz;
  }

  /**
   * Provides orientation of deflector.
   *
   * @return orientation
   */
  public Rotation getOrientation() {
    return this.orientation;
  }

  /**
   * Assigns the current scan angle. The method checks the maximal allowed scan
   * angle. If the current scan angle is larger then the method will set it to
   * the maximal allowed value.
   *
   * @param scanAngleInRad current scan angle (Allowed range 0..maximalDeflectionAngleInRad)
   */
  public void setScanAngleInRad(double scanAngleInRad) {

    // Check scan angle range
    if (scanAngleInRad < 0) {
      scanAngleInRad = 0;
    } else if (scanAngleInRad > this.maximalDeflectionAngleInRad) {
      scanAngleInRad = this.maximalDeflectionAngleInRad;
    }

    this.currentScanAngleInRad = scanAngleInRad;
  }

  /**
   * Assign the scan frequency, This frequency is used to calculate the scan angle step width.
   * The scan angle step width is used by doSimStep().
   *
   * @param scanFreqInHz \todo what is the different between pulse and scan frequency?
   */
  public void setScanFreq_Hz(double scanFreqInHz) {
    this.scanFreqInHz = scanFreqInHz;
  }

  /**
   * Runs simulation step of the deflector. This will generate a new orientation for pulse emitter.
   */
  public abstract void doSimStep();

  /**
   * Marks the deflector has send the last pulse out.
   *
   * @return True last pulse is send
   */
  public abstract boolean hasLastPulseLeftDevice();

}
