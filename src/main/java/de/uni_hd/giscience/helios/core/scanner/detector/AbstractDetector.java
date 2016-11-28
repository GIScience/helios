package de.uni_hd.giscience.helios.core.scanner.detector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import de.uni_hd.giscience.helios.core.MeasurementsBuffer;
import de.uni_hd.giscience.helios.core.scanner.Measurement;
import de.uni_hd.giscience.helios.core.scanner.Scanner;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public abstract class AbstractDetector {

  Scanner scanner = null;

  public double cfg_device_accuracy_m = 0;
  public double cfg_device_rangeMin_m = 0;
  public int cfg_setting_beamSampleQuality = 1;

  // File output:
  final String outputFileLineFormatString = "%.3f %.3f %.3f %.4f \"%s\"";
  BufferedWriter mPointsFileWriter = null;

  public MeasurementsBuffer mBuffer;

  public AbstractDetector(Scanner scanner, double accuracy_m, double rangeMin_m) {

    this.cfg_device_accuracy_m = accuracy_m;
    this.cfg_device_rangeMin_m = rangeMin_m;

    this.scanner = scanner;
  }


  // ATTENTION: This method needs to be synchronized since multiple threads are writing to the output file!
  public synchronized void setOutputFilePath(String path) {

    // ATTENTION: There's a chance that closing the pointsFileWriter triggers an exception since other
    // threads might still write to it.

    if (mPointsFileWriter != null) {
      try {
        mPointsFileWriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (path.equals("")) {
      mPointsFileWriter = null;
      return;
    }

    File outputFilePath = new File(path);

    try {

      outputFilePath.getParentFile().mkdirs();

      mPointsFileWriter = new BufferedWriter(new FileWriter(outputFilePath, true), 500000);
    } catch (Exception e) {
      mPointsFileWriter = null;

      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

  public synchronized void shutdown() {

    if (mPointsFileWriter != null) {
      try {
        mPointsFileWriter.flush();
        mPointsFileWriter.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // TODO(Sebastian Bechtold): Move writing of output files to separate classes
  public synchronized void writeMeasurement(Measurement m) {
    if (mPointsFileWriter != null) {

      // TODO(KoeMai) Why is the measure position gets a offset from the platform?
      Vector3D shifted = m.position.add(scanner.platform.scene.getShift());

      String line = String.format(outputFileLineFormatString, shifted.getX(), shifted.getY(), shifted.getZ(), m.intensity, m.hitObjectId);

      line += "\n";

      try {
        mPointsFileWriter.write(line);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public abstract void applySettings(ScannerSettings settings);

  public abstract void simulatePulse(
          ExecutorService execService,
          Vector3D absoluteBeamOrigin,
          Rotation absoluteBeamAttitude,
          int currentPulseNumber,
          long currentGpsTime);

}
