package de.uni_hd.giscience.helios.core.scanner.detector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.scanner.Measurement;
/**
 * The abstract ray pulse runnable is a worker/task which calculates points and attributes for a beam pulse.
 * The calculation is realized in the speciallization of the run()-function. This function can calculate points
 * be analysing the beam-vector(orientation and attitude).
 */
public abstract class AbstractPulseRunnable implements Runnable {

  // ############## BEGIN Static variables ###############
  // TODO(Sebastian Bechtold): Move this to a central place?
  static final Vector3D forward = new Vector3D(0, 1, 0);
  static final Vector3D right = new Vector3D(1, 0, 0);

  // Speed of light in m/nanosec:
  static final double cfg_speedOfLight_mPerNanosec = 0.299792458;
  // ############## END Static variables ###############

  /**
   * Reference to detector for detector properties.
   */
  AbstractDetector detector = null;

  /**
   * Orientation of the detector.
   */
  Vector3D absoluteBeamOrigin;

  /**
   * Attitude of the detector.
   */
  Rotation absoluteBeamAttitude;

  /**
   * Count of pulses in detector.
   */
  int currentPulseNum;

  /**
   * Timestamp of measurement.
   */
  Long currentGpsTime;

  /**
   * The variable 'boxmuller_use_last' is needed for the box-muller gaussian random number generator.
   */
  boolean boxmuller_use_last = false;

  double boxMullerRandom(double d, double e) {
    /*
     * Implements the Polar form of the Box-Muller Transformation (c) Copyright 1994,
     * Everett F. Carter Jr. Permission is granted by the author to use this software for any
     * application provided this copyright notice is preserved. Source: https://www.taygeta.com/random/boxmuller.html
     */

    double x1, x2, w, y1;
    double y2 = 0;

    if (boxmuller_use_last) {
      y1 = y2;
      boxmuller_use_last = false;
    } else {
      do {
        x1 = 2.0 * Math.random() - 1.0;
        x2 = 2.0 * Math.random() - 1.0;
        w = x1 * x1 + x2 * x2;
      } while (w >= 1.0);

      w = Math.sqrt((-2.0 * Math.log(w)) / w);
      y1 = x1 * w;
      y2 = x2 * w;
      boxmuller_use_last = true;
    }

    return (d + y1 * e);
  }

  public AbstractPulseRunnable(
          AbstractDetector detector,
          Vector3D absoluteBeamOrigin,
          Rotation absoluteBeamAttitude,
          int pulseNumber,
          Long gpsTime) {
    this.detector = detector;
    this.absoluteBeamAttitude = absoluteBeamAttitude;
    this.absoluteBeamOrigin = absoluteBeamOrigin;
    this.currentPulseNum = pulseNumber;
    this.currentGpsTime = gpsTime;
  }

  // LiDAR energy equation (fine tuning required) + spatial distribution equation
  double calcIntensity(double incidenceAngle, double distance, double reflect_p, double radius) {
    // input parameters to intensity calculation
    double Pt = 500; // peak transmitted energy at center of the beam profile
    double eta_sys = 0.9; // LiDAR scanner efficiency

    double eta_atm = 1;

    double A = reflect_p;
    double B = 1 - reflect_p;
    double D = 0.075 * 2; // receiver aperture diameter [m]
    double reflectanceBRDF = ((Math.PI * D * D) / 4.0)
            * ((A / Math.pow(Math.cos(incidenceAngle), 6))
            * Math.exp(Math.tan(incidenceAngle)
            * Math.tan(incidenceAngle))
            + B * Math.cos(incidenceAngle));

    // transmitted energy with 'radius' away from center of the beam profile
    double wavelength = 1550.0 / 1000000.0;
    double w0 = (2 * wavelength) / (Math.PI * detector.scanner.cfg_device_beamDivergence_rad);
    double omega = (distance * wavelength) / (Math.PI * w0 * w0);
    double omega0 = 1 - distance / detector.cfg_device_rangeMin_m;
    double w = w0 * Math.sqrt(omega * omega + omega0 * omega0);
    double Pt2 = Pt * ((w0 / w) * (w0 / w)) * Math.exp((-2 * radius * radius) / (w * w));

    // output intensity (based on: Carlsson et al, Signature simulation and signal analysis for 3-D laser radar, 2001)
    return (Pt2 * reflectanceBRDF * Math.cos(incidenceAngle) * eta_atm * eta_sys);
  }

  /**
   * Calculates the coordinate and writes point attributes to a file and measure buffer.
   *
   * @param beamOrigin
   * @param beamDir
   * @param distance      Distance between scanner and this calculated point
   * @param intensity     Intensity of the point
   * @param returnNumber  ?
   * @param fullwaveIndex ?
   * @param hitObjectId   ?
   */
  void capturePoint(Vector3D beamOrigin, Vector3D beamDir,
                    double distance, double intensity,
                    int returnNumber, int fullwaveIndex,
                    String hitObjectId) {

    // Abort if point distance is below mininum scanner range:
    if (distance < detector.cfg_device_rangeMin_m) {
      return;
    }

    distance = reduceDistanceAccuracy(distance);

    // Calculate final recorded point coordinates:
    Vector3D pointPos = beamOrigin.add(beamDir.scalarMultiply(distance));

    Measurement m = new Measurement();
    m.position = pointPos;
    m.distance = distance;
    m.intensity = intensity;
    m.returnNumber = returnNumber;
    m.fullwaveIndex = fullwaveIndex;
    m.beamOrigin = beamOrigin;
    m.beamDirection = beamDir;
    m.hitObjectId = hitObjectId;

    detector.writeMeasurement(m);

    detector.mBuffer.add(m);
  }

  /**
   * Apply gaussian range accuracy error to the distance
   *
   * @param distance accurate distance
   * @return distance with error
   */
  private double reduceDistanceAccuracy(double distance) {
    double precision = this.detector.cfg_device_accuracy_m / 2;
    double error = -precision + Math.random() * (precision * 2);
    error += boxMullerRandom(0.0, this.detector.cfg_device_accuracy_m / 2);

    distance += error;
    return distance;
  }

  /**
   * Function which calculate points on beam
   */
  public abstract void run();

}
