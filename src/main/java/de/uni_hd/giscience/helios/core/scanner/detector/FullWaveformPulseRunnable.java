// TODO 2: Tell Niko about radius calculation

package de.uni_hd.giscience.helios.core.scanner.detector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.scene.RaySceneIntersection;
import de.uni_hd.giscience.helios.core.scene.Scene;

public class FullWaveformPulseRunnable extends AbstractPulseRunnable {

	FullWaveformPulseDetector fwDetector;

	public FullWaveformPulseRunnable(FullWaveformPulseDetector detector, Vector3D absoluteBeamOrigin, Rotation absoluteBeamAttitude, int currentPulseNum, long currentGpsTime) {

		super((AbstractDetector) detector, absoluteBeamOrigin, absoluteBeamAttitude, currentPulseNum, currentGpsTime);

		fwDetector = detector;
	}

	// time distribution equation required to calculate the pulse beam energy increasing and decreasing in time
	// (based on: Carlsson et al, Signature simulation and signal analysis for 3-D laser radar, 2001)
	private int timeWaveFunction(ArrayList<Double> timeWave, int numBins) {
		double ns_step = detector.scanner.getPulseLength_ns() / (double) numBins;
		double tau = (detector.scanner.getPulseLength_ns() * 0.5) / 3.5;

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

	private void captureFullWave(ArrayList<Double> fullwave, int fullwaveIndex, double max_time, Vector3D beamOrigin, Vector3D beamDir, Long gpstime) {
		// add noise to fullwave
		double precision = 0.001 * Collections.max(fullwave);
		//double error = -precision + Math.random() * (precision * 2);
		//error += boxMullerRandom(0.0, this.detector.cfg_device_accuracy_m / 2);
		// for (Double f : fullwave) f+=error;

		fwDetector.writeFullWave(fullwave, fullwaveIndex, max_time, beamOrigin, beamDir, gpstime);
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

		double radiusStep_rad = this.fwDetector.scanner.cfg_device_beamDivergence_rad / this.fwDetector.cfg_setting_beamSampleQuality;

		// ######## BEGIN Outer loop over radius steps from beam center to outer edge ##############
		for (int radiusStep = 0; radiusStep < fwDetector.cfg_setting_beamSampleQuality; radiusStep++) {

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

		Iterator<Entry<Double, Double>> iter = reflections.entrySet().iterator();

		while (iter.hasNext()) {
			Entry<Double, Double> entry = (Entry<Double, Double>) iter.next();

			double entryDistance = entry.getKey();

			if (entryDistance > maxHitDist_m) {
				maxHitDist_m = entryDistance;
			}
		}

		if (maxHitDist_m < 0) {
			detector.scanner.setLastPulseWasHit(false);
			return;
		}
		// ############ END Step 1: Find maximum hit distance, abort if nothing was hit #############

		// ############ BEGIN Create full waveform #############

		// 2. create full waveform with t_max entries

		// TODO 3: Make these values configurable in XML
		int cfg_numTimeBins = 50; // discretize the time wave into 100 bins (time_step = beam pulse length [ns] / 100)
		int cfg_numFullwaveBins = 500; // discretize the time full waveform into 200 bins (time_step = total_time [ns] / 200)

		ArrayList<Double> time_wave = new ArrayList<Double>();
		int peakIntensityIndex = timeWaveFunction(time_wave, cfg_numTimeBins);

		ArrayList<Double> fullwave = new ArrayList<Double>(Collections.nCopies(cfg_numFullwaveBins, 0.0));

		// 3. calc time at maximum distance (i.e. total beam time)
		double maxHitTime_ns = maxHitDist_m / cfg_speedOfLight_mPerNanosec + detector.scanner.getPulseLength_ns(); // [ns]

		// 4. multiply each sub-beam intensity with time_wave and add to the full waveform
		iter = reflections.entrySet().iterator();

		// ########### BEGIN Iterate over waveform data ###########
		while (iter.hasNext()) {
			Entry<Double, Double> entry = (Entry<Double, Double>) iter.next();

			// Vector3D sub_beam_dir=waveform_beamdir.get(w);
			double entryDistance_m = entry.getKey();
			double entryIntensity = entry.getValue();

			double wavePeakTime_ns = (entryDistance_m / cfg_speedOfLight_mPerNanosec); // [ns]

			double blubb = (detector.scanner.getPulseLength_ns() / (double) cfg_numTimeBins);

			// Old loops:
			/*
			 * // Loop from start of wave to peak (actually, backwards from peak to start): for (int i = 0; i < peakIntensityIndex; ++i) {
			 * 
			 * double time_tmp = wavePeakTime_ns - (peakIntensityIndex - i) * blubb; // [ns]
			 * 
			 * int fullwaveBinIndex = (int) ((time_tmp / maxHitTime_ns) * cfg_numFullwaveBins); fullwave.set(fullwaveBinIndex, time_wave.get(i) * entryIntensity); }
			 * 
			 * // Loop from peak of wave to end: for (int i = peakIntensityIndex, j = 0; i < time_wave.size(); ++i, ++j) {
			 * 
			 * double time_tmp = wavePeakTime_ns + (j) * blubb; // [ns]
			 * 
			 * int fullwaveBinIndex = (int) ((time_tmp / maxHitTime_ns) * cfg_numFullwaveBins); fullwave.set(fullwaveBinIndex, time_wave.get(i) * entryIntensity); }
			 */

			// New Loop:

			double time_start = wavePeakTime_ns - (peakIntensityIndex * blubb);

			for (int i = 0; i < time_wave.size(); ++i) {

				double time_tmp = time_start + i * blubb;

				int fullwaveBinIndex = (int) ((time_tmp / maxHitTime_ns) * cfg_numFullwaveBins);
				fullwave.set(fullwaveBinIndex, time_wave.get(i) * entryIntensity);
			}
		}
		// TODO 2: Can multiple returns overlap here?

		// ########### END Iterate over waveform data ###########

		// ############### BEGIN Calculate average intensity ############
		double avgIntensity = 0;
		int numPositive = 0;

		for (int i = 0; i < fullwave.size(); ++i) {
			if (fullwave.get(i) > 0) {
				avgIntensity += fullwave.get(i);
				numPositive++;
			}
		}

		if (numPositive == 0) {
			return;
		}

		avgIntensity /= (double) numPositive;
		// ############### END Calculate average intensity ############

		// ############ BEGIN Extract points from waveform data ################
		int num_returns = 0;

		int win_size = 10; // search for peaks around [-win_size, win_size]

		for (int i = 1; i < fullwave.size() - 1; ++i) {

			// TODO 2: Ask Niko why intensity needs to be above average to be a peak
			if (fullwave.get(i) > avgIntensity) {

				boolean hasPeak = false;

				for (int j = Math.max(0, i - win_size); j < Math.min(fullwave.size(), i + win_size); j++) {

					// TODO 1: Understand this
					if (fullwave.get(i) < fullwave.get(j)) {

						hasPeak = true;
						break;
					}
				}

				if (hasPeak) {
					double distance = cfg_speedOfLight_mPerNanosec * ((i / (double) cfg_numFullwaveBins) * maxHitTime_ns);

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

					capturePoint(absoluteBeamOrigin, beamDir, distance, fullwave.get(i), num_returns + 1, currentPulseNum, hitObject);

					++num_returns;
				}
			}
		}

		// ############ END Extract points from waveform data ################

		if (num_returns > 0) {
			detector.scanner.setLastPulseWasHit(true);
			captureFullWave(fullwave, currentPulseNum, maxHitTime_ns, absoluteBeamOrigin, beamDir, currentGpsTime);
		}

		// ############ END Create full waveform ##############
	}

}
