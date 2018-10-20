package de.uni_hd.giscience.helios.core.scanner;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class ScannerHead {

	// Device definition:
	public Vector3D cfg_device_rotateAxis = new Vector3D(1, 0, 0);
	private double cfg_device_rotatePerSecMax_rad = 0;

	// Settings:
	private double cfg_setting_rotatePerSec_rad = 0;
	private double cfg_setting_rotateStop_rad = 0;
	private double cfg_setting_rotateStart_rad = 0;
	private double cfg_setting_rotateRange_rad = 0;
	
	// State variables:
	private double state_currentRotateAngle_rad = 0;

	// Cache variables:
	private Rotation cached_mountRelativeAttitude = new Rotation(new Vector3D(0, 1, 0), 0);

	public ScannerHead(Vector3D headRotationAxis, double headRotatePerSecMax_rad) {
		this.cfg_device_rotateAxis = headRotationAxis;
		this.cfg_device_rotatePerSecMax_rad = headRotatePerSecMax_rad;
	}

	
	public void applySettings(ScannerSettings settings) {

		this.setRotatePerSec_rad(settings.headRotatePerSec_rad);
		this.setCurrentRotateAngle_rad(settings.headRotateStart_rad);

		this.cfg_setting_rotateStop_rad = settings.headRotateStop_rad;
		this.cfg_setting_rotateStart_rad = settings.headRotateStart_rad;
		this.cfg_setting_rotateRange_rad = Math.abs(cfg_setting_rotateStart_rad - cfg_setting_rotateStop_rad);
            System.out.println("Horizontal resolution: " + (settings.headRotatePerSec_rad * 180 / Math.PI) / settings.scanFreq_Hz);
        }


	public void doSimStep(double pulseFreq_Hz) {

		if (cfg_setting_rotatePerSec_rad != 0) {
			setCurrentRotateAngle_rad(state_currentRotateAngle_rad + cfg_setting_rotatePerSec_rad / pulseFreq_Hz);
		}
	}

	public Rotation getMountRelativeAttitude() {
		return this.cached_mountRelativeAttitude;
	}

	public boolean rotateCompleted() {

		boolean result = false;

		if (cfg_setting_rotatePerSec_rad < 0) {
			result = state_currentRotateAngle_rad <= cfg_setting_rotateStop_rad;
		} else {
			result = state_currentRotateAngle_rad >= cfg_setting_rotateStop_rad;
		}

		//System.out.println(state_currentRotateAngle_rad + " " + cfg_setting_rotateStop_rad);
				
		return result;
	}

	public void setCurrentRotateAngle_rad(double angle_rad) {

		if (angle_rad == state_currentRotateAngle_rad)
			return;

		state_currentRotateAngle_rad = angle_rad;
		cached_mountRelativeAttitude = new Rotation(cfg_device_rotateAxis, state_currentRotateAngle_rad % (2 * Math.PI));
	}

	public void setRotatePerSec_rad(double rotateSpeed_rad) {

		// Limit head rotate speed to device maximum:
		if (Math.abs(rotateSpeed_rad) > this.cfg_device_rotatePerSecMax_rad) {
			rotateSpeed_rad = Math.signum(rotateSpeed_rad) * this.cfg_device_rotatePerSecMax_rad;
		}

		this.cfg_setting_rotatePerSec_rad = rotateSpeed_rad;
	}
	
	public double getRotateStart() {
		return this.cfg_setting_rotateStart_rad;
	}
	
	public double getRotateRange() {
		return this.cfg_setting_rotateRange_rad;
	}
	
	public double getRotateCurrent() {
		return this.state_currentRotateAngle_rad;
	}
}
