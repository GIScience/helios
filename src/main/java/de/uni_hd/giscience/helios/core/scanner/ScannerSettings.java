package de.uni_hd.giscience.helios.core.scanner;

import de.uni_hd.giscience.helios.core.Asset;

public class ScannerSettings extends Asset {

	public Boolean active = true;
	public Integer beamSampleQuality = 5;
	public Double headRotatePerSec_rad = 0d;
	public Double headRotateStart_rad = 0d;
	public Double headRotateStop_rad = 0d;
	public Integer pulseFreq_Hz = 0;
	public double scanAngle_rad = 0;
	public Integer scanFreq_Hz = 0;
	public Double beamDivAngle=0.003;
	public double pulseLength_ns = 0;

	public ScannerSettings() {

	}

	
	//Copy constructor:
	public ScannerSettings(ScannerSettings other) {
		
		if (other == null) return;
		
		this.active = other.active;
		this.beamSampleQuality = other.beamSampleQuality;
		this.headRotatePerSec_rad = other.headRotatePerSec_rad;
		this.headRotateStart_rad = other.headRotateStart_rad;
		this.headRotateStop_rad = other.headRotateStop_rad;
		this.pulseFreq_Hz = other.pulseFreq_Hz;
		this.scanAngle_rad = other.scanAngle_rad;
		this.scanFreq_Hz = other.scanFreq_Hz;
		this.beamDivAngle = other.beamDivAngle;
		this.pulseLength_ns = other.pulseLength_ns;

	}
}