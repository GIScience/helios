// TODO 2: Fix scan angle getting out of control under certain (unknown) circumstances

package de.uni_hd.giscience.helios.core.scanner;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.Asset;
import de.uni_hd.giscience.helios.core.platform.Platform;
import de.uni_hd.giscience.helios.core.scanner.beamDeflector.AbstractBeamDeflector;
import de.uni_hd.giscience.helios.core.scanner.detector.AbstractDetector;

public class Scanner extends Asset {

	public ScannerHead scannerHead = null;
	public AbstractBeamDeflector beamDeflector = null;
	public Platform platform = null;
	public AbstractDetector detector = null;

	// FWF settings
	public FWFSettings FWF_settings = null;
	//
	
	// Misc:
	public String cfg_device_visModelPath = "";

	// ########## BEGIN Emitter ###########
	public Vector3D cfg_device_headRelativeEmitterPosition = new Vector3D(0, 0, 0);
	public Rotation cfg_device_headRelativeEmitterAttitude = new Rotation(new Vector3D(1, 0, 0), 0);
	ArrayList<Integer> cfg_device_supportedPulseFreqs_Hz = new ArrayList<Integer>();
	public double cfg_device_beamDivergence_rad = 0;
	double cfg_device_pulseLength_ns = 0;
	int cfg_setting_pulseFreq_Hz = 0;
	public String cfg_device_id = "";
	public double cfg_device_averagePower_w;
	public double cfg_device_beamQuality;
	public double cfg_device_efficiency;
	public double cfg_device_receiverDiameter_m;
	public double cfg_device_visibility_km;
	public double cfg_device_wavelength_m;
	
	public double atmosphericExtinction;
	public double beamWaistRadius;
	// ########## END Emitter ###########

	// State variables:
	int state_currentPulseNumber = 0;
	boolean state_lastPulseWasHit = false;
	boolean state_isActive = true;
	
	// Cached variables
	public double cached_Dr2;
	public double cached_Bt2;

	
	public Scanner(double beamDiv_rad, Vector3D beamOrigin, Rotation beamOrientation, ArrayList<Integer> pulseFreqs, double pulseLength_ns, String visModel, 
			String id, double averagePower, double beamQuality, double efficiency, double receiverDiameter, double atmosphericVisibility, int wavelength) {
		
		// Configure emitter:
		this.cfg_device_headRelativeEmitterPosition = beamOrigin;
		this.cfg_device_headRelativeEmitterAttitude = beamOrientation;
		this.cfg_device_supportedPulseFreqs_Hz = pulseFreqs;
		this.cfg_device_beamDivergence_rad = beamDiv_rad;
		this.cfg_device_pulseLength_ns = pulseLength_ns;
		this.cfg_device_id = id;
		this.cfg_device_averagePower_w = averagePower;
		this.cfg_device_beamQuality = beamQuality;			
		this.cfg_device_efficiency = efficiency;
		this.cfg_device_receiverDiameter_m = receiverDiameter;
		this.cfg_device_visibility_km = atmosphericVisibility;
		this.cfg_device_wavelength_m = wavelength / 1000000000f;
		
		this.atmosphericExtinction = calcAtmosphericAttenuation();
		this.beamWaistRadius = (cfg_device_beamQuality * cfg_device_wavelength_m) / (Math.PI * cfg_device_beamDivergence_rad);	
		
		// Configure misc:
		this.cfg_device_visModelPath = visModel;
		
		// Precompute variables
		this.cached_Dr2 = cfg_device_receiverDiameter_m * cfg_device_receiverDiameter_m;
		this.cached_Bt2 = cfg_device_beamDivergence_rad * cfg_device_beamDivergence_rad;
			
		System.out.println(this.toString());
	}
	
	public void applySettings(ScannerSettings settings) {

		// Configure scanner:
		this.setActive(settings.active);
		this.setPulseFreq_Hz(settings.pulseFreq_Hz);

		detector.applySettings(settings);
		scannerHead.applySettings(settings);
		beamDeflector.applySettings(settings);
	}

	public void applySettingsFWF(FWFSettings settings) {
		FWF_settings=settings;
		
	}

	public void doSimStep(ExecutorService execService) {

		// Update head attitude (we do this even when the scanner is inactive):
		scannerHead.doSimStep(cfg_setting_pulseFreq_Hz);

		// If the scanner is inactive, stop here:
		if (!state_isActive) {
			return;
		}

		// Update beam deflector attitude:
		this.beamDeflector.doSimStep();

		if (!beamDeflector.lastPulseLeftDevice()) {
			return;
		}

		// Global pulse counter:
		state_currentPulseNumber++;

		// Calculate absolute beam origin:
		Vector3D absoluteBeamOrigin = this.platform.getAbsoluteMountPosition().add(cfg_device_headRelativeEmitterPosition);

		// Calculate absolute beam attitude:
		Rotation mountRelativeEmitterAttitude = this.scannerHead.getMountRelativeAttitude().applyTo(this.cfg_device_headRelativeEmitterAttitude);
		Rotation absoluteBeamAttitude = platform.getAbsoluteMountAttitude().applyTo(mountRelativeEmitterAttitude).applyTo(beamDeflector.getEmitterRelativeAttitude());

		// Caclulate time of the emitted pulse
		Long unixTime = System.currentTimeMillis() / 1000L;
		Long currentGpsTime = (unixTime - 315360000) - 1000000000;

		detector.simulatePulse(execService, absoluteBeamOrigin, absoluteBeamAttitude, state_currentPulseNumber, currentGpsTime);
	}

	
	public int getPulseFreq_Hz() {
		return this.cfg_setting_pulseFreq_Hz;
	}

	public double getPulseLength_ns() {
		return this.cfg_device_pulseLength_ns;
	}

	
	public boolean lastPulseWasHit() {
		return this.state_lastPulseWasHit;
	}

	public boolean isActive() {
		return this.state_isActive;
	}

	public void setActive(boolean active) {
		state_isActive = active;
	}
	
	public double calcFootprintArea(double distance) {
		double Bt2 = cached_Bt2;
		double R = distance;
		
		return (Math.PI * R * R * Bt2) / 4;
	}
	
	public double calcFootprintRadius(double distance) {  // TODO Jorge: This is overkill
		double area = calcFootprintArea(distance);
		
		return Math.sqrt(area / Math.PI);
	}
	
	// Simulate energy loss from aerial particles (Carlsson et al., 2001)
	private double calcAtmosphericAttenuation() {  
		double q;
		
		double wavelenth_nm = cfg_device_wavelength_m * 1000000000f;
		
		if (wavelenth_nm < 500 && wavelenth_nm > 2000) {
			return 0;	// Do no nothing if wavelength is outside this range as the approximation will be bad
		}				  
		
		if (cfg_device_visibility_km > 50) 
			q = 1.6; 
		else if (cfg_device_visibility_km > 6 && cfg_device_visibility_km < 50) 
			q = 1.3; 
		else 
			q = 0.585 * Math.pow(cfg_device_visibility_km, 0.33);		
		
		return (3.91 / cfg_device_visibility_km) * Math.pow((wavelenth_nm / 0.55), -q);
  	}


	public void setPulseFreq_Hz(int pulseFreq_Hz) {

		// Check of requested pulse freq is > 0:
		if (pulseFreq_Hz < 0) {
			System.out.println("ERROR: Attempted to set pulse frequency < 0. This is not possible.");
			pulseFreq_Hz = 0;
		}

		// Check if requested pulse freq is supported by device:
		if (!cfg_device_supportedPulseFreqs_Hz.contains(pulseFreq_Hz)) {
			System.out.println("WARNING: Specified pulse frequency is not supported by this device. We'll set it nevertheless.");
		}

		// Set new pulse frequency:
		this.cfg_setting_pulseFreq_Hz = pulseFreq_Hz;

		//System.out.println("Pulse frequency set to " + this.cfg_setting_pulseFreq_Hz);
	}
	
	public void setLastPulseWasHit(boolean value) {
		
		if (value == state_lastPulseWasHit) return;
		
		synchronized(this) {
			this.state_lastPulseWasHit = value;
		}
	}
	
	@Override
	public String toString() {
		return "SCANNER: " + cfg_device_id + " " + 
				"Power: " + Double.toString(cfg_device_averagePower_w) + " W " + 
				"Divergence: " + Double.toString(cfg_device_beamDivergence_rad * 1000) + " mrad " + 
				"Wavelength: " + Double.toString(cfg_device_wavelength_m * Math.pow(10, 9)) + " nm " + 
				"Visibility: " + Double.toString(cfg_device_visibility_km) + " km";
	}
}
