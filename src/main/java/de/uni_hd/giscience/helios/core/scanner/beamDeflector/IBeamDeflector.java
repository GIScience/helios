package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

/**
 * This interface is used to realize different beam deflectors for simulation.
 *
 * A beam deflector in a lidar system can provide 1 or multiply emitter beam
 * attitudes. In the the simulation each emitter beam is simulated separate.
 * This makes it necessary to detect the end of a beam deflector simulation for
 * a motion position.
 *
 * This interface provides the simulation required functions to run a simulation.
 */
public interface IBeamDeflector {

  /**
   * Assigns the settings for simulation of a beam deflector.
   * This settings are used for a complete simulation.
   * Modification at run time of simulation will reset the internal state.
   *
   * @param settings scanner settings
   */
  void applySettings(ScannerSettings settings);

  /**
   * Provides the rotation by the beam deflector of the emitted beam.
   *
   * @return present rotation matrix for the emitted beam
   */
  Rotation getEmitterRelativeAttitude();

  /**
   * Runs a simulation step of the beam deflector.
   * <p>
   * The simulation step calculates the next rotation matrix for a emitted beam.
   * This simulation step can be a complete motion step or a sub step.
   * <p>
   * For example a polygon mirror beam deflector has for each simulation step also
   * a motion step of the scanner. A fiber array beam deflector provides for one
   * motion step multiply emitted beams ( sub steps)
   */
  void doSimStep();

  /**
   * Provides information about complete simulation for a motion step.
   *
   * @return TRUE motion step is completed,
   * FALSE motion step needs mor simulation steps
   */
  boolean HasLastPulseLeftDevice();
}
