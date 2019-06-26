package de.uni_hd.giscience.helios.core.scanner.detector;

import java.util.Random;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.LasSpecification;
import de.uni_hd.giscience.helios.core.scanner.Measurement;

public abstract class AbstractPulseRunnable implements Runnable {

	// ############## BEGIN Static variables ###############
	// Speed of light in m/sec:
	final static double speedOfLight_mPerSec = 299792458;

	// Speed of light in m/nanosec:
	final static double cfg_speedOfLight_mPerNanosec = 0.299792458;

	// Speed of light in m/picosec:
	final static double speedOfLight_mPerPicosec = 0.000299792458;
	// ############## END Static variables ###############

	AbstractDetector detector = null;

	Vector3D absoluteBeamOrigin;
	Rotation absoluteBeamAttitude;

	int currentPulseNum;
	Long currentGpsTime;
        
        int scanDirection;

	// The variable 'boxmuller_use_last' is needed for the box-muller gaussian random number generator:
	boolean boxmuller_use_last = false;
	boolean writeGround = true;

	double boxMullerRandom(double d, double e) {

		/*
		 * Implements the Polar form of the Box-Muller Transformation (c) Copyright 1994, Everett F. Carter Jr. Permission is granted by the author to use this software for any
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

	public AbstractPulseRunnable(AbstractDetector detector, Vector3D absoluteBeamOrigin, Rotation absoluteBeamAttitude, int pulseNumber, Long gpsTime, int scanDirection) {
		this.detector = detector;
		this.absoluteBeamAttitude = absoluteBeamAttitude;
		this.absoluteBeamOrigin = absoluteBeamOrigin;
		this.currentPulseNum = pulseNumber;
		this.currentGpsTime = gpsTime;
                this.scanDirection = scanDirection;
	}
	
	// Generate Gaussian-distributed error for more realistic intensity
	double calcErrorFactor(double stdev) {
		
		double mean = 1;
		Random random = new Random();	
		return random.nextGaussian() * stdev + mean;	
	}
	
	// Calculate the strength of the laser going back to the detector
	double calcIntensity(double incidenceAngle, double targetRange, double targetReflectivity, double targetSpecularity, double targetArea) {	      
		
		double emmitedPower = detector.scanner.getAveragePower();
		double intensity = calcReceivedPower(emmitedPower, targetRange, incidenceAngle, targetReflectivity, targetSpecularity, targetArea);
		double etaErr = calcErrorFactor(0.10);	
		
		return intensity * etaErr * 1000000000f; // TODO Jorge: Values are so small; should be normalized to 0-255 range	
	}

	// ALS Simplification "Radiometric Calibration..." (Wagner, 2010) Eq. 14
	double calcCrossSection(double f, double Alf, double theta) {
		
		return 4 * Math.PI * f * Alf * Math.cos(theta);
	}
	
	// Phong reflection model "Normalization of Lidar Intensity..." (Jutzi and Gross, 2009) 
	double phongBDRF(double incidenceAngle, double targetSpecularity) {
		
		double ks = targetSpecularity;	
		double kd = (1 - ks);	
		double n = 10;	//TODO Jorge: Each material should have its own glossiness

		double diffuse = kd * Math.cos(incidenceAngle);
		double specular = ks * Math.pow(Math.cos(2 * incidenceAngle), n);
		
		return diffuse + specular;
	}

	// Energy left after attenuation by air particles in range [0,1]
	double calcAtmosphericFactor(double targetRange) {  
		
	  	return Math.exp(-2 * targetRange * detector.scanner.getAtmosphericExtinction()); 
  	}
	
	// Laser radar equation "Signature simulation..." (Carlsson et al., 2000)
	double calcReceivedPower(double emmitedPower, double targetRange, double incidenceAngle, double targetReflectivity, double targetSpecularity, double targetArea) {			
		
		double Pt = emmitedPower;
		double Dr2 = detector.scanner.getDr2();
		double R = targetRange;
		double Bt2 = detector.scanner.getBt2();
		double etaSys = detector.scanner.getEfficiency();	
		double etaAtm = calcAtmosphericFactor(targetRange);
		double bdrf = targetReflectivity * phongBDRF(incidenceAngle, targetSpecularity);
		double sigma = calcCrossSection(bdrf, targetArea, incidenceAngle);
	
		return (Pt * Dr2) / ( 4 * Math.PI * Math.pow(R, 4) * Bt2) * etaSys * etaAtm * sigma;	
	}

	void capturePoint(Vector3D beamOrigin, Vector3D beamDir, double distance, double intensity, double echo_width, int returnNumber, int pulseReturnNumber, int fullwaveIndex, String hitObjectId, int classification) {

		// Abort if point distance is below mininum scanner range:
		if (distance < detector.cfg_device_rangeMin_m) {
			return;
		}

		// ########## BEGIN Apply gaussian range accuracy error ###########
		double precision = this.detector.cfg_device_accuracy_m / 2;
		double error = -precision + Math.random() * (precision * 2);
		error += boxMullerRandom(0.0, this.detector.cfg_device_accuracy_m / 2);
		distance += error;
		// ########## END Apply gaussian range accuracy error ###########

		// Calculate final recorded point coordinates:
		Vector3D pointPos = beamOrigin.add(beamDir.scalarMultiply(distance));

		Measurement m = new Measurement();
		m.position = pointPos;
		m.distance = distance;
		m.intensity = intensity;
		m.echo_width = echo_width;
		m.returnNumber = returnNumber;
		m.pulseReturnNumber = pulseReturnNumber;
		m.fullwaveIndex = fullwaveIndex;
		m.beamOrigin = beamOrigin;
		m.beamDirection = beamDir;
		m.hitObjectId = hitObjectId;
		m.classification = classification;
                m.scanDirFlag = scanDirection;

		detector.writeMeasurement(m);

		detector.mBuffer.add(m);
	}

	void capturePoint(Measurement m) {
		
		if(!writeGround && m.classification == LasSpecification.GROUND) {
			return;
		}		
		
		// Abort if point distance is below mininum scanner range:
		if (m.distance < detector.cfg_device_rangeMin_m) {
			return;
		}

		// ########## BEGIN Apply gaussian range accuracy error ###########
		double precision = this.detector.cfg_device_accuracy_m / 2;
		double error = -precision + Math.random() * (precision * 2);
		error += boxMullerRandom(0.0, this.detector.cfg_device_accuracy_m / 2);
		m.distance += error;
		// ########## END Apply gaussian range accuracy error ###########

		// Calculate final recorded point coordinates:
		m.position = m.beamOrigin.add(m.beamDirection.scalarMultiply(m.distance));

		detector.writeMeasurement(m);

		detector.mBuffer.add(m);
	}
	
	public abstract void run();

}
