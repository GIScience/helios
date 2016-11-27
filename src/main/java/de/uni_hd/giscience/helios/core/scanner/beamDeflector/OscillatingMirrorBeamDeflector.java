package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public class OscillatingMirrorBeamDeflector extends AbstractBeamDeflector {

  public OscillatingMirrorBeamDeflector(double scanAngleMax_rad, int scanProduct) {
    super(scanAngleMax_rad);

    this.cfg_device_scanProduct = scanProduct;
	}

	int cfg_device_scanProduct = 1000000;

	int currentScanLinePulse = 0;

	int cached_pulsesPerScanline = 0;

	@Override
	public void applySettings(ScannerSettings settings) {
		super.applySettings(settings);

      rotationAngleBetweenPulsesInRad = (this.scanFreqInHz * this.currentScanAngleInRad * 4) / settings.pulseFreq_Hz;
      cached_pulsesPerScanline = (int) (((double) settings.pulseFreq_Hz) / this.scanFreqInHz);
    }

	
	@Override
	public void doSimStep() {

		currentScanLinePulse++;

		if (currentScanLinePulse == cached_pulsesPerScanline) {
			currentScanLinePulse = 0;
		}

		// Update beam angle:
		int bla = Math.min(currentScanLinePulse, cached_pulsesPerScanline / 2) - Math.max(0, currentScanLinePulse - cached_pulsesPerScanline / 2);

      currentBeamAngleInRad = -this.currentScanAngleInRad + rotationAngleBetweenPulsesInRad * bla;

		// Rotate to current position:
      this.orientation = new Rotation(Directions.right, currentBeamAngleInRad);
    }

	@Override
    public void setScanAngleInRad(double scanAngle_rad) {

		double scanAngle_deg = scanAngle_rad * (180.0 / Math.PI);

		// Max. scan angle is limited by scan product:
      if (scanAngle_deg * this.scanFreqInHz > this.cfg_device_scanProduct) {
        System.out.println("ERROR: Requested scan angle exceeds device limitations as defined by scan product. Will set it to maximal possible value.");
        scanAngle_deg = ((double) this.cfg_device_scanProduct) / this.scanFreqInHz;
      }

      this.currentScanAngleInRad = scanAngle_deg * (Math.PI / 180);

		System.out.println("Scan angle set to " + scanAngle_deg + " degrees.");
	}

	@Override
	public void setScanFreq_Hz(double scanFreq_Hz) {

		// Max. scan frequency is limited by scan product:
      if (this.currentScanAngleInRad * (180.0 / Math.PI) * scanFreq_Hz > this.cfg_device_scanProduct) {
        System.out.println("ERROR: Requested scan frequency exceeds device limitations as defined by scan product. Will set it to maximal possible value.");
        scanFreq_Hz = ((double) this.cfg_device_scanProduct) / (this.currentScanAngleInRad * (180.0 / Math.PI));
      }

      this.scanFreqInHz = scanFreq_Hz;

      System.out.println("Scan frequency set to " + this.scanFreqInHz + " Hz.");
    }

  @Override
  public boolean hasLastPulseLeftDevice() {
    return true;
  }
}
