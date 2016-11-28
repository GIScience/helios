// TODO(Sebastian Bechtold): Fix scan angle getting out of control under certain (unknown) circumstances

package de.uni_hd.giscience.helios.core.scanner;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import de.uni_hd.giscience.helios.core.Asset;
import de.uni_hd.giscience.helios.core.platform.Platform;
import de.uni_hd.giscience.helios.core.scanner.beamDeflector.AbstractBeamDeflector;
import de.uni_hd.giscience.helios.core.scanner.detector.AbstractDetector;

/**
 * The scanner represents the lidar scanning system.
 * This scanner gets information about the flight position/orientation form the platform
 * and combines it with the scanning head for the beam origin and orientation on emit.
 */
public class Scanner extends Asset {

  public ScannerHead scannerHead = null;
  public AbstractBeamDeflector beamDeflector = null;
  public Platform platform = null;
  public AbstractDetector detector = null;

  // Misc:
  public String cfg_device_visModelPath = "";

  // ########## BEGIN Emitter ###########
  public Vector3D position = new Vector3D(0, 0, 0);
  public Rotation orientation = new Rotation(new Vector3D(1, 0, 0), 0);
  ArrayList<Integer> cfg_device_supportedPulseFreqs_Hz = new ArrayList<Integer>();
  public double cfg_device_beamDivergence_rad = 0;
  double cfg_device_pulseLength_ns = 0;

  int pulseFrequencyInHz = 0;
  // ########## END Emitter ###########

  // State variables:
  int state_currentPulseNumber = 0;
  boolean state_lastPulseWasHit = false;
  boolean isScannerActive = true;


  public Scanner(
          double beamDiv_rad,
          Vector3D position,
          Rotation orientation,
          ArrayList<Integer> pulseFreqs,
          double pulseLength_ns, String visModel) {

    this.position = position;
    this.orientation = orientation;
    this.cfg_device_supportedPulseFreqs_Hz = pulseFreqs;
    this.cfg_device_beamDivergence_rad = beamDiv_rad;
    this.cfg_device_pulseLength_ns = pulseLength_ns;

    // Configure misc:
    this.cfg_device_visModelPath = visModel;
  }

  public void applySettings(ScannerSettings settings) {

    // Configure scanner:
    this.setActive(settings.active);
    this.setPulseFreq_Hz(settings.pulseFreq_Hz);

    detector.applySettings(settings);
    scannerHead.applySettings(settings);
    beamDeflector.applySettings(settings);
  }


  /**
   * The do simulation step function calculates for one simulation step the position and
   * orientation for beam emitting and detects the reflected beam from the environment
   * by the detector.
   *
   * @param execService Used for sharing thread pool
   */
  public void doSimStep(ExecutorService execService) {

    // Update head attitude (we do this even when the scanner is inactive):
    try {
      scannerHead.doSimStep(pulseFrequencyInHz);
    } catch (Exception e) {
      System.out.println("WARNING: PulseFrequence is to small.");
    }

    // If the scanner is inactive, stop here:
    if (!isScannerActive) {
      return;
    }

    // Update beam deflector attitude:
    this.beamDeflector.doSimStep();

    if (!beamDeflector.hasLastPulseLeftDevice()) {
      return;
    }

    // Global pulse counter:
    state_currentPulseNumber++;

    // Calculate absolute beam origin:
    Vector3D absoluteBeamOrigin = platform.getAbsoluteMountPosition().add(position);

    // Calculate absolute beam orientation:
    Rotation beamOrientationInScannerSystem = this.scannerHead.getHeadOrientation().applyTo(this.orientation);
    Rotation beamOrientationInEnvironmentSystem = platform.getAbsoluteMountAttitude()
            .applyTo(beamOrientationInScannerSystem)
            .applyTo(beamDeflector.getOrientation());

    // Calculate time of the emitted pulse
    // \todo(KoeMai) Why is the emitted pulse using the system clock for simulation is this a good idea?
    // The timestamps are not reproduced in test setup for verification of software.
    Long unixTime = System.currentTimeMillis() / 1000L;
    Long currentGpsTime = (unixTime - 315360000) - 1000000000;

    detector.simulatePulse(execService, absoluteBeamOrigin, beamOrientationInEnvironmentSystem, state_currentPulseNumber, currentGpsTime);
  }


  public int getPulseFreq_Hz() {
    return this.pulseFrequencyInHz;
  }

  public double getPulseLength_ns() {
    return this.cfg_device_pulseLength_ns;
  }


  public boolean lastPulseWasHit() {
    return this.state_lastPulseWasHit;
  }

  public boolean isActive() {
    return this.isScannerActive;
  }

  public void setActive(boolean active) {
    isScannerActive = active;
  }


  public void setPulseFreq_Hz(int pulseFrequencyInHz) {

    // Check of requested pulse freq is > 0:
    if (pulseFrequencyInHz < 0) {
      System.out.println("ERROR: Attempted to set pulse frequency < 0. This is not possible.");
      pulseFrequencyInHz = 0;
    }

    // Check if requested pulse freq is supported by device:
    if (!cfg_device_supportedPulseFreqs_Hz.contains(pulseFrequencyInHz)) {
      System.out.println("WARNING: Specified pulse frequency is not supported by this device. We'll set it nevertheless.");
    }

    // Set new pulse frequency:
    this.pulseFrequencyInHz = pulseFrequencyInHz;
  }

  public void setLastPulseWasHit(boolean value) {

    if (value == state_lastPulseWasHit) {
      return;
    }

    synchronized (this) {
      this.state_lastPulseWasHit = value;
    }
  }
}
