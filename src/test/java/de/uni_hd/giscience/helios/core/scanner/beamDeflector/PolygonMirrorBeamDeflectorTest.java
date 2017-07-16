package de.uni_hd.giscience.helios.core.scanner.beamDeflector;


import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class PolygonMirrorBeamDeflectorTest {


  public enum ScanSteps {
    Small(5), Large(500);

    private int numVal;

    ScanSteps(int numVal) {
      this.numVal = numVal;
    }

    public int getScanFrequency() {
      return numVal;
    }
  }


  private PolygonMirrorBeamDeflector createMirror(
          double maxSupportedScanAngleInDegree,
          double maxScanAngleInDegree,
          ScanSteps stepWide) {
    PolygonMirrorBeamDeflector mirror = new PolygonMirrorBeamDeflector(
            0,
            0,
            maxSupportedScanAngleInDegree / 360 * (2 * Math.PI),
            0);
    ScannerSettings settings = new ScannerSettings();
    try {

      settings.pulseFreq_Hz = (1000);
      settings.scanAngle_rad = (maxScanAngleInDegree / 360 * (2 * Math.PI));
      settings.scanFreq_Hz = stepWide.getScanFrequency();
    } catch (Exception ex) {
      assertFalse(true, "error: " + ex.getMessage());
    }

    mirror.applySettings(settings);
    return mirror;
  }

  private double GetRotationAngleFromAttitudeInDegree(PolygonMirrorBeamDeflector mirror) {
    Rotation r = mirror.getEmitterRelativeAttitude();
    return r.getAngles(RotationOrder.XYZ)[0] * 360 / (2 * Math.PI);
  }

  @Test
  void testStartRotation() {
    PolygonMirrorBeamDeflector mirror = createMirror(
            100,
            90,
            ScanSteps.Small
    );

    for (int i = 0; i < 5000; i++) {
      mirror.doSimStep();

      double angle = GetRotationAngleFromAttitudeInDegree(mirror);
      System.out.print(angle + ", ");
    }
  }

  @Test
  void testMultiplySimulationSteps() {
    PolygonMirrorBeamDeflector mirror = createMirror(
            100,
            100,
            ScanSteps.Small
    );

    mirror.doSimStep();
    double firstScanAngle = GetRotationAngleFromAttitudeInDegree(mirror);

    mirror.doSimStep();
    double nextScanAngle = GetRotationAngleFromAttitudeInDegree(mirror);

    mirror.doSimStep();
    double next2ScanAngle = GetRotationAngleFromAttitudeInDegree(mirror);

    double firstDist = nextScanAngle - firstScanAngle;
    double nextDist = next2ScanAngle - nextScanAngle;

    assertEquals(
            firstDist,
            nextDist,
            0.00000001,
            "Simulation steps have different distances");
  }

  @Test
  void testStartAngle() {
    PolygonMirrorBeamDeflector mirror = createMirror(
            100,
            100,
            ScanSteps.Small
    );

    assertEquals(-100.0, GetRotationAngleFromAttitudeInDegree(mirror));
  }

  @Test
  void testSmallSimulationStepWithoutOverrun() {
    PolygonMirrorBeamDeflector mirror = createMirror(
            100,
            100,
            ScanSteps.Small
    );

    mirror.doSimStep();

    assertEquals(
            -99.0,
            GetRotationAngleFromAttitudeInDegree(mirror),
            0.0000001);

  }

  @Test
  void testLargeSimulationStepWithoutOverrunOfRotation() {
    PolygonMirrorBeamDeflector mirror = createMirror(
            100,
            100,
            ScanSteps.Large
    );

    mirror.doSimStep();

    assertEquals(
            0,
            GetRotationAngleFromAttitudeInDegree(mirror),
            0.0000001);
  }

  @Test
  void testOverrunOfRotationToNegativeMaxScanAngle() {
    PolygonMirrorBeamDeflector mirror = createMirror(
            100,
            100,
            ScanSteps.Small
    );

    for (int steps = 0; steps < 200; steps++) {
      mirror.doSimStep();
    }

    assertEquals(
            -100.0,
            GetRotationAngleFromAttitudeInDegree(mirror),
            0.0000001);
  }

  @Test
  void testOverrunOfRotationToNegativeMaxScanAngleWithDifferentMax() {
    PolygonMirrorBeamDeflector mirror = createMirror(
            100,
            50,
            ScanSteps.Small
    );

    for (int steps = 0; steps < 200; steps++) {
      mirror.doSimStep();
    }

    assertEquals(
            -50.0,
            GetRotationAngleFromAttitudeInDegree(mirror),
            0.0000001);
  }

  @Test
  void testLargeSimulationStepsWithOverrunOfRotation() {
    PolygonMirrorBeamDeflector mirror = createMirror(
            100,
            100,
            ScanSteps.Large
    );

    mirror.doSimStep();
    mirror.doSimStep();

    assertEquals(
            -100,
            GetRotationAngleFromAttitudeInDegree(mirror),
            0.0000001);
  }

  @Test
  void testMultiplyOverrunOfRotationInOneSimulationStep() {
    PolygonMirrorBeamDeflector mirror = new PolygonMirrorBeamDeflector(
            0,
            0,
            100.0 / 360 * (2 * Math.PI),
            0);
    ScannerSettings settings = new ScannerSettings();
    try {

      settings.pulseFreq_Hz = (444);
      settings.scanAngle_rad = (100.0 / 360 * (2 * Math.PI));
      settings.scanFreq_Hz = 1000;
    } catch (Exception ex) {
      assertFalse(true, "error: " + ex.getMessage());
    }

    mirror.applySettings(settings);

    mirror.doSimStep();

    assertEquals(
            -49.549549549,
            GetRotationAngleFromAttitudeInDegree(mirror),
            0.0000001);
  }

  @Test
  void testHasLastPulseLeftDeviceForMultiplyStep() {
    PolygonMirrorBeamDeflector mirror = createMirror(
            100,
            100,
            ScanSteps.Small
    );

    mirror.doSimStep();
    assertTrue(mirror.HasLastPulseLeftDevice());

    mirror.doSimStep();
    assertTrue(mirror.HasLastPulseLeftDevice());

    mirror.doSimStep();
    assertTrue(mirror.HasLastPulseLeftDevice());
  }

  // \todo (KoeMai) Add tests for configuration warnings
}