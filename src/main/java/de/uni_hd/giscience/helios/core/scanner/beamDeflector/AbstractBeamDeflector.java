package de.uni_hd.giscience.helios.core.scanner.beamDeflector;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public abstract class AbstractBeamDeflector {

	// Device definition variables:
	double cfg_device_scanFreqMax_Hz = 0;
	double cfg_device_scanFreqMin_Hz = 0;
	double cfg_device_scanAngleMax_rad = 0;

	// Setting variables:
	double cfg_setting_scanFreq_Hz = 0;
	double cfg_setting_scanAngle_rad = 0;

	// Stat variables:
	double state_currentBeamAngle_rad = 0;
        
        int scanDirFlag = 0;

	public AbstractBeamDeflector(double scanAngleMax_rad, double scanFreqMax_Hz, double scanFreqMin_Hz) {
		this.cfg_device_scanAngleMax_rad = scanAngleMax_rad;
		this.cfg_device_scanFreqMax_Hz = scanFreqMax_Hz;
		this.cfg_device_scanFreqMin_Hz = scanFreqMin_Hz;

	}
	
	// ############# BEGIN "state cache" members ##############
	// Members with "_cached" prefix contain values that are derived from state variables and
	// required multiple times per sim step at different places in the code. In order to avoid
	// unneccessary re-computations of the same value, they are cached in special variables:
	double cached_angleBetweenPulses_rad;
	Rotation cached_emitterRelativeAttitude = new Rotation(new Vector3D(1, 0, 0), 0);
	// ############# END "state cache" members ##############

	public void applySettings(ScannerSettings settings) {

		setScanAngle_rad(settings.scanAngle_rad);
		setScanFreq_Hz(settings.scanFreq_Hz);
		
		state_currentBeamAngle_rad = 0;
		cached_angleBetweenPulses_rad = (double) (this.cfg_setting_scanFreq_Hz * this.cfg_setting_scanAngle_rad * 2) / settings.pulseFreq_Hz;
	} 

	public Rotation getEmitterRelativeAttitude() {
		return this.cached_emitterRelativeAttitude;
	}


	abstract public void doSimStep();

	public void setScanAngle_rad(double scanAngle_rad) {

		if (scanAngle_rad < 0) {
			scanAngle_rad = 0;
		} else if (scanAngle_rad > this.cfg_device_scanAngleMax_rad) {
			System.out.println("Scan angle " + scanAngle_rad * (180.0 / Math.PI) + " higher than supported " + this.cfg_device_scanAngleMax_rad * (180.0 / Math.PI) + ". Setting to max supported: " + this.cfg_device_scanAngleMax_rad * (180.0 / Math.PI) );
			scanAngle_rad = this.cfg_device_scanAngleMax_rad;
		}

		this.cfg_setting_scanAngle_rad = scanAngle_rad;
	}

	public boolean lastPulseLeftDevice() {
		return true;
	}

	public void setScanFreq_Hz(double scanFreq_hz) {
		this.cfg_setting_scanFreq_Hz = scanFreq_hz;
	}
        
        public int getScanDirection() {
                return this.scanDirFlag;
        }
}
