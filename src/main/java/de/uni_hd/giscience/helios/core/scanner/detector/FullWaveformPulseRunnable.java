package de.uni_hd.giscience.helios.core.scanner.detector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.scanner.Measurement;
import de.uni_hd.giscience.helios.core.scene.RaySceneIntersection;
import de.uni_hd.giscience.helios.core.scene.Scene;

import org.orangepalantir.leastsquares.Fitter;
import org.orangepalantir.leastsquares.Function;
import org.orangepalantir.leastsquares.fitters.MarquardtFitter;
import org.orangepalantir.leastsquares.fitters.NonLinearSolver;


public class FullWaveformPulseRunnable extends AbstractPulseRunnable {
    Function gaussianModel = new Function(){
        @Override
        public double evaluate(double[] values, double[] parameters) {
            double A = parameters[0];
            double B = parameters[1];
            double C = parameters[2];
            double D = parameters[3];
            double x = values[0];
            return A + B * Math.exp( - Math.pow(((x-C)/D), 2) );
        }
        @Override
        public int getNParameters() {
            return 4;
        }

        @Override
        public int getNInputs() {
            return 1;
        }
    };

	FullWaveformPulseDetector fwDetector;

	public FullWaveformPulseRunnable(FullWaveformPulseDetector detector, Vector3D absoluteBeamOrigin, Rotation absoluteBeamAttitude, int currentPulseNum, long currentGpsTime) {

		super((AbstractDetector) detector, absoluteBeamOrigin, absoluteBeamAttitude, currentPulseNum, currentGpsTime);

		fwDetector = detector;
	}

	/*
	 * private double calcAtmosphericAttenuation(double range, double visibilityDistance, double wavelength) { // Calculate the aerosol scattering (based on: Steinvall, Waveform
	 * simulation for 3-d sensing laser radars, 2000) double q; if (visibilityDistance > 50) q = 1.6; else if (visibilityDistance > 6 && visibilityDistance < 50) q = 1.3; else q =
	 * 0.585 * Math.pow(visibilityDistance, 0.33);
	 * 
	 * double AER = (3.91 / visibilityDistance) * Math.pow((wavelength / 0.55), -q);
	 * 
	 * return (Math.exp(-2 * range * AER)); }
	 */

	// LiDAR energy equation (fine tuning required) + spatial distribution equation
	double calcIntensity(double incidenceAngle, double distance, double reflect_p, double radius) {
		// input parameters to intensity calculation
		double Pt = detector.scanner.FWF_settings.peakEnergy; // peak transmitted energy at center of the beam profile
		double eta_sys = detector.scanner.FWF_settings.scannerEfficiency; // LiDAR scanner efficiency

		// double visibilityDistance = 100; // atmospheric visibility [km]
		double eta_atm = 1.0; //detector.scanner.FWF_settings.atmosphericVisibility; // calcAtmosphericAttenuation(distance, visibilityDistance, wavelength); // atmospheric attenuation
/*
		double A = reflect_p;
		double B = 1 - reflect_p;
		double D = detector.scanner.FWF_settings.apartureDiameter; // receiver aperture diameter [m]
		double reflectanceBRDF = ((Math.PI * D * D) / 4.0)
				* ((A / Math.pow(Math.cos(incidenceAngle), 6)) * Math.exp(Math.tan(incidenceAngle) * Math.tan(incidenceAngle)) + B * Math.cos(incidenceAngle));
*/
		// transmitted energy with 'radius' away from center of the beam profile
		double wavelength = detector.scanner.FWF_settings.scannerWaveLength / 1000000.0;
		double w0 = (2 * wavelength) / (Math.PI * detector.scanner.FWF_settings.beamDivergence_rad);
		double omega = (distance * wavelength) / (Math.PI * w0 * w0);
		double omega0 = 1 - distance / detector.cfg_device_rangeMin_m;
		double w = w0 * Math.sqrt(omega * omega + omega0 * omega0);
		double Pt2 = Pt * ((w0 / w) * (w0 / w)) * Math.exp((-2 * radius * radius) / (w * w));

		// output intensity (based on: Carlsson et al, Signature simulation and signal analysis for 3-D laser radar, 2001)
		double intensity=Pt2 * reflect_p * (Math.cos(incidenceAngle)) * eta_atm * eta_sys;
		
		return (intensity);
	}

	// time distribution equation required to calculate the pulse beam energy increasing and decreasing in time
	// (based on: Carlsson et al, Signature simulation and signal analysis for 3-D laser radar, 2001)
	private int timeWaveFunction(ArrayList<Double> timeWave, int numBins) {
		double ns_step = detector.scanner.FWF_settings.pulseLength_ns / (double) numBins;
		double tau = (detector.scanner.FWF_settings.pulseLength_ns * 0.5) / 3.5;

		int peakIntensityIndex = 0;
		double peakIntensityTmp = 0;

		for (int i = 0; i < numBins; ++i) {
			timeWave.add((((i * ns_step) / tau) * ((i * ns_step) / tau)) * Math.exp(-(i * ns_step) / tau));
			if (timeWave.get(i) > peakIntensityTmp) {
				peakIntensityTmp = timeWave.get(i);
				peakIntensityIndex = i;
			}
		}

		return (peakIntensityIndex);
	}

	private void captureFullWave(ArrayList<Double> fullwave, int fullwaveIndex, double min_time, double max_time, Vector3D beamOrigin, Vector3D beamDir, Long gpstime) {
		// add noise to fullwave
		/*
		double precision = 0.01 * Collections.max(fullwave);
		double error = -precision + Math.random() * (precision * 2);
		error += boxMullerRandom(0.0, this.detector.cfg_device_accuracy_m / 2);
		 for (Double f : fullwave) f+=error;
		*/
		fwDetector.writeFullWave(fullwave, fullwaveIndex, min_time, max_time, beamOrigin, beamDir, gpstime);
	}

	@Override
	public void run() {
		Scene scene = detector.scanner.platform.scene;

		Vector3D beamDir = absoluteBeamAttitude.applyTo(forward);

		// Early abort if central axis of the beam does not intersect with the scene:

		// NOTE:
		// With beam divergence / full waveform being simulated, this is not perfect, since a sub-ray
		// might hit the scene even if the central ray does now. However, this check is a very important
		// performance optimization, so we should keep it nevertheless. sbecht 2016-04-24

		double[] tMinMax = scene.getAABB().getRayIntersection(absoluteBeamOrigin, beamDir);

		if (tMinMax == null) {
			detector.scanner.setLastPulseWasHit(false);
			return;
		}

		// ##################### BEGIN Perform racasting for each sub-ray and find all intersections #################

		TreeMap<Double, Double> reflections = new TreeMap<Double, Double>();

		// List to store all intersections. This is required later to reconstruct the associations between extracted points
		// and the scene objects that caused them:
		ArrayList<RaySceneIntersection> intersects = new ArrayList<>();

		double radiusStep_rad =  detector.scanner.FWF_settings.beamDivergence_rad / detector.scanner.FWF_settings.beamSampleQuality;

		// ######## BEGIN Outer loop over radius steps from beam center to outer edge ##############
		for (int radiusStep = 0; radiusStep < detector.scanner.FWF_settings.beamSampleQuality; radiusStep++) {

			double subrayDivergenceAngle_rad = radiusStep * radiusStep_rad;

			// Rotate subbeam into divergence step (towards outer rim of the beam cone):
			Rotation r1 = new Rotation(right, subrayDivergenceAngle_rad);

			// Calculate circle step width:
			int circleSteps = (int) (2 * Math.PI) * radiusStep;

			// Make sure that central ray is not skipped:
			if (circleSteps == 0) {
				circleSteps = 1;
			}

			double circleStep_rad = (2 * Math.PI) / circleSteps;

			// ######## BEGIN Inner loop over sub-rays along the circle ##############
			for (int circleStep = 0; circleStep < circleSteps; circleStep++) {

				// Rotate around the circle:
				Rotation r2 = new Rotation(forward, circleStep_rad * circleStep).applyTo(r1);

				Vector3D subrayDirection = absoluteBeamAttitude.applyTo(r2).applyTo(forward);

				RaySceneIntersection intersect = scene.getIntersection(absoluteBeamOrigin, subrayDirection, false);

				if (intersect != null && intersect.prim != null) {

					intersects.add(intersect);

					// Incidence angle:
					double incidenceAngle = intersect.prim.getIncidenceAngle_rad(absoluteBeamOrigin, subrayDirection);

					// Distance between beam origin and intersection:
					double distance = intersect.point.distance(absoluteBeamOrigin);
					
					if(detector.cfg_device_rangeMin_m>distance) continue;

					// Distance between the beam's center line and the intersection point:
					double radius = Math.sin(subrayDivergenceAngle_rad) * distance;

					double intensity = calcIntensity(incidenceAngle, distance, intersect.prim.material.reflectance, radius);

					reflections.put(distance, intensity);
				}
			}
			// ######## END Inner loop over sub-rays along the circle ##############
		}
		// ######## END Outer loop over radius steps from beam center to outer edge ##############

		// ##################### END Perform racasting for each sub-ray and find all intersections #################

		// ############ BEGIN Step 1: Find maximum hit distance, abort if nothing was hit #############
		double maxHitDist_m = 0;
		double minHitDist_m = 9999999;

		Iterator<Entry<Double, Double>> iter = reflections.entrySet().iterator();

		while (iter.hasNext()) {
			Entry<Double, Double> entry = (Entry<Double, Double>) iter.next();

			double entryDistance = entry.getKey();

			if (entryDistance > maxHitDist_m) {
				maxHitDist_m = entryDistance;
			}
			if (entryDistance < minHitDist_m) {
				minHitDist_m = entryDistance;
			}
		}

		if (maxHitDist_m < 0) {
			detector.scanner.setLastPulseWasHit(false);
			return;
		}
		// ############ END Step 1: Find maximum hit distance, abort if nothing was hit #############

		// ############ BEGIN Create full waveform #############

		// 2. create full waveform with t_max entries

		int cfg_numTimeBins = detector.scanner.FWF_settings.numTimeBins; // discretize the time wave into X bins (time_step = beam pulse length [ns] / X)
		int cfg_numFullwaveBins = detector.scanner.FWF_settings.numFullwaveBins; // discretize the time full waveform into X bins (time_step = total_time [ns] / X)

		ArrayList<Double> time_wave = new ArrayList<Double>();
		int peakIntensityIndex = timeWaveFunction(time_wave, cfg_numTimeBins);

		ArrayList<Double> fullwave = new ArrayList<Double>(Collections.nCopies(cfg_numFullwaveBins, 0.0));

		// Gaussian model fitting init
		double[][] xs = new double[fullwave.size()][1];
		for(int i=0;i<fullwave.size();++i) xs[i][0]=((double)i);
		double[] zs = new double[fullwave.size()];
		//
    	
		// 3. calc time at minimum and maximum distance (i.e. total beam time in fwf signal)
		double maxHitTime_ns = maxHitDist_m / cfg_speedOfLight_mPerNanosec + detector.scanner.FWF_settings.pulseLength_ns; // [ns]
		
		double minHitTime_ns= minHitDist_m / cfg_speedOfLight_mPerNanosec - detector.scanner.FWF_settings.pulseLength_ns;

		if(detector.cfg_device_rangeMin_m/0.299792458 > minHitTime_ns) {
			return;
		}

		// 4. multiply each sub-beam intensity with time_wave and add to the full waveform

		// ########### BEGIN Iterate over waveform data ###########
		iter = reflections.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Double, Double> entry = (Entry<Double, Double>) iter.next();

			// Vector3D sub_beam_dir=waveform_beamdir.get(w);
			double entryDistance_m = entry.getKey();
			double entryIntensity = entry.getValue();

			double wavePeakTime_ns = (entryDistance_m / cfg_speedOfLight_mPerNanosec); // [ns]

			double blubb = (detector.scanner.FWF_settings.pulseLength_ns / (double) cfg_numTimeBins);

			double time_start = wavePeakTime_ns - (peakIntensityIndex * blubb);

			for (int i = 0; i < time_wave.size(); ++i) {

				double time_tmp = time_start + i * blubb;

				int fullwaveBinIndex = (int) (((time_tmp-minHitTime_ns) / (maxHitTime_ns-minHitTime_ns)) * cfg_numFullwaveBins);
				fullwave.set(fullwaveBinIndex, time_wave.get(i) * entryIntensity);
			}
		}

		// ########### END Iterate over waveform data ###########

		// ############ BEGIN Extract points from waveform data via Gaussian decomposition ################
		int num_returns = 0;
		int win_size = detector.scanner.FWF_settings.winSize; // search for peaks around [-win_size, win_size]
		//double min_wave_width=detector.scanner.FWF_settings.minEchoWidth; // [ns]

		for (int i=0; i < fullwave.size(); ++i) {
			zs[i]=fullwave.get(i);
		}

		Fitter fit = new MarquardtFitter(gaussianModel);
		fit.setData(xs, zs);
		
		ArrayList<Measurement> PointsMeasurement=new ArrayList<Measurement>(); // temp solution
        
		for (int i = 0; i < fullwave.size(); ++i) {
			if(fullwave.get(i)<0.001) continue;
			
			// peak detection
			boolean hasPeak = true;
			for (int j = Math.max(0, i - 1); j > Math.max(0, i - win_size); j--) {
				if (fullwave.get(j)<0.001 || fullwave.get(j) >= fullwave.get(i)) {
					hasPeak = false;
					break;
				}
			}
			if(hasPeak) {
				for (int j = Math.min(fullwave.size(), i + 1); j < Math.min(fullwave.size(), i + win_size); j++) {
					if (fullwave.get(j)<0.001 ||fullwave.get(j) >= fullwave.get(i)) {
						hasPeak = false;
						break;
					}
				}
			}

			if (hasPeak) {
				// Gaussian model fitting
		        fit.setParameters(new double[]{0, fullwave.get(i), i, 1});
		        try {
		        	fit.fitData();
		        } catch (java.lang.RuntimeException e) { 
					continue;
				}
		        double echo_width = (double)fit.getParameters()[3];
		        echo_width=(echo_width/cfg_numFullwaveBins)*(maxHitTime_ns-minHitTime_ns);

				if(echo_width<0.1)  {  //min_wave_width) {
					continue;
				}
					
				// ########## END echo location, intensity and width extraction via Gaussian decomposition ###########
				double distance = cfg_speedOfLight_mPerNanosec * ((i / (double) cfg_numFullwaveBins) * (maxHitTime_ns-minHitTime_ns)+minHitTime_ns);

				// ########## BEGIN Build list of objects that produced this return ###########
			
				double minDifference = Double.MAX_VALUE;
				RaySceneIntersection closestIntersection = null;

				for (RaySceneIntersection intersect : intersects) {
					double intersectDist = intersect.point.distance(absoluteBeamOrigin);

					if (Math.abs(intersectDist - distance) < minDifference) {
						minDifference = Math.abs(intersectDist - distance);
						closestIntersection = intersect;
					}
				}

				String hitObject = null;
				if (closestIntersection != null) {
					//objects.add(closestIntersection.prim.part.id);
					hitObject = closestIntersection.prim.part.mId;
				}
				// ########## END Build list of objects that produced this return ###########
				Measurement tmp=new Measurement();
				tmp.beamOrigin=absoluteBeamOrigin;
				tmp.beamDirection=beamDir;
				tmp.distance=distance;
				tmp.echo_width=echo_width;
				tmp.intensity= fullwave.get(i);
				tmp.fullwaveIndex=currentPulseNum;
				tmp.hitObjectId=hitObject;
				tmp.returnNumber=num_returns + 1;
				PointsMeasurement.add(tmp);

				++num_returns;
			}
		}

		// ############ END Extract points from waveform data ################

		if (num_returns > 0) {
			for(int i=0;i<PointsMeasurement.size();++i)  {
				PointsMeasurement.get(i).pulseReturnNumber=num_returns;
				capturePoint(PointsMeasurement.get(i));
			}

			detector.scanner.setLastPulseWasHit(true);
			captureFullWave(fullwave, currentPulseNum, minHitTime_ns, maxHitTime_ns, absoluteBeamOrigin, beamDir, currentGpsTime);
		}

		// ############ END Create full waveform ##############
	}

}
