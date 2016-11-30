package de.uni_hd.giscience.helios.surveyplayback;

import de.uni_hd.giscience.helios.core.platform.PlatformSettings;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

/**
 * The leg is collection of information which are used for a simulation stage.
 * A leg is used for simulation a scan from a start position.
 */
public class Leg {

	/**
	 * Scanner settings of this leg
	 */
	public ScannerSettings mScannerSettings = null;
	/**
	 * Settings for the plattform on this leg
	 */
	public PlatformSettings mPlatformSettings = null;
	
	public Leg() {
		mScannerSettings = new ScannerSettings();
		mPlatformSettings = new PlatformSettings();
	}

	public Leg( ScannerSettings scanSettings, PlatformSettings plattSettings) {
		mScannerSettings = scanSettings;
		mPlatformSettings = plattSettings;
	}
}
