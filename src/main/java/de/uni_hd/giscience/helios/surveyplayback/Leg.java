package de.uni_hd.giscience.helios.surveyplayback;

import de.uni_hd.giscience.helios.core.platform.PlatformSettings;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public class Leg {

	// Scanner Settings:
	public ScannerSettings mScannerSettings = null;
	public PlatformSettings mPlatformSettings = null;
	
	private double length = 0;	// Distance to the next leg 
	
	public Leg() {
		
	}
	
	public double getLength() {
		return this.length;
	}
	
	public void setLength(double length) {
		this.length = length;
	}
}
