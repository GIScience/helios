package de.uni_hd.giscience.helios.core.scanner.detector;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import static com.vividsolutions.jts.geom.CoordinateSequence.M;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.assetsloading.ScenePart;
import de.uni_hd.giscience.helios.core.scanner.Measurement;
import de.uni_hd.giscience.helios.core.scene.Material;
import de.uni_hd.giscience.helios.core.scene.RaySceneIntersection;
import de.uni_hd.giscience.helios.core.scene.Scene;
import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;
import de.uni_hd.giscience.helios.core.scene.primitives.Sphere;
import de.uni_hd.giscience.helios.core.scene.primitives.Triangle;
import de.uni_hd.giscience.helios.core.scene.primitives.Vertex;

import org.orangepalantir.leastsquares.Fitter;
import org.orangepalantir.leastsquares.Function;
import org.orangepalantir.leastsquares.fitters.MarquardtFitter;


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
    final double t_step = detector.scanner.FWF_settings.pulseLength_ns / (double) detector.scanner.FWF_settings.numTimeBins;
    final double tau = (detector.scanner.FWF_settings.pulseLength_ns * 0.5) / 3.5;		
    final double blubb = detector.scanner.FWF_settings.pulseLength_ns / (double) detector.scanner.FWF_settings.numTimeBins;
    final double cfg_device_timeMin_ns=  detector.cfg_device_rangeMin_m / cfg_speedOfLight_mPerNanosec;
    
	public FullWaveformPulseRunnable(FullWaveformPulseDetector detector, Vector3D absoluteBeamOrigin, Rotation absoluteBeamAttitude, int currentPulseNum, long currentGpsTime, Node rootNode) {

		super((AbstractDetector) detector, absoluteBeamOrigin, absoluteBeamAttitude, currentPulseNum, currentGpsTime, rootNode);

		fwDetector = detector;
	}

	// Space distribution equation to calculate the beam energy decreasing the further away from the center (Carlsson et al., 2001)
	private double calcEmmitedPower(double radius, double targetRange) {
		double I0 = detector.scanner.getAveragePower();
		double lambda = detector.scanner.getWavelenth();
		double R = targetRange;
		double R0 = detector.cfg_device_rangeMin_m;
		double r = radius;
		double w0 = detector.scanner.getBeamWaistRadius();
		
		double denom = Math.PI * w0 * w0;
		double omega = (lambda * R) / denom;
		double omega0 = (lambda * R0) / denom;
		double w = w0 * Math.sqrt(omega0 * omega0 + omega * omega);
		
		return I0 * Math.exp((-2 * r * r) / (w * w));
	}
	
	// Calculate the strength of the laser going back to the detector
	double calcIntensity(double incidenceAngle, double targetRange, double targetReflectivity, double targetSpecularity, double targetArea, double radius) {			
		double emmitedPower = calcEmmitedPower(radius, targetRange);
		double intensity = super.calcReceivedPower(emmitedPower, targetRange, incidenceAngle, targetReflectivity, targetSpecularity, targetArea);
		return intensity * 1000000000f;
	}
			
	// Time distribution equation to calculate the pulse beam energy increasing and decreasing in time (Carlsson et al., 2001)
	protected int calcTimeWave(ArrayList<Double> timeWave) {
		int peakIndex = -1;
        double peakValue = 0;
        double t, t_tau, pt;	
		
		for (int i = 0; i < timeWave.size(); i++) {
			t = i * t_step;
			t_tau = t / tau;
			pt = (t_tau * t_tau) * Math.exp(-t_tau);
			timeWave.set(i, pt);
			if (pt > peakValue) {
				peakValue = pt;
				peakIndex = i;
			}
		}

		return peakIndex;
	}
    
    // Multiply each sub-beam intensity with time wave and add to the full waveform 
    protected ArrayList<Double> calcFullWaveform(final TreeMap<Double, Double> reflections, final double maxHitTime_ns, final double minHitTime_ns, final double totalHitTime_ns) {	     
        final int numTimeBins = detector.scanner.FWF_settings.numTimeBins;
        final int numFullwaveBins = detector.scanner.FWF_settings.numFullwaveBins;
        
        ArrayList<Double> timeWave = new ArrayList<>(Collections.nCopies(numTimeBins, 0.0));      
		ArrayList<Double> fullwave = new ArrayList<>(Collections.nCopies(numFullwaveBins, 0.0));
        final int peakIntensityIndex = calcTimeWave(timeWave); 
        
        Iterator<Entry<Double, Double>> iter = reflections.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Double, Double> entry = (Entry<Double, Double>) iter.next();
			double distance_m = entry.getKey();
			double intensity = entry.getValue();
			double wavePeakTime_ns = (distance_m / cfg_speedOfLight_mPerNanosec);		
			double timeStart = wavePeakTime_ns - (peakIntensityIndex * blubb);		
            for (int i = 0; i < timeWave.size(); i++) {
				double timeTmp = timeStart + i * blubb;
				int fullWaveBinIndex = (int) (((timeTmp - minHitTime_ns) / totalHitTime_ns) * numFullwaveBins);
				fullwave.set(fullWaveBinIndex, timeWave.get(i) * intensity);
			}
		}

        return fullwave;
    }
    
    Fitter initGaussianFitter(final ArrayList<Double> fullwave) {
        double[][] xs = new double[fullwave.size()][1];
        double[] zs = new double[fullwave.size()];     
        for (int i = 0; i < fullwave.size(); i++) {
            xs[i][0] = (double) i;
            zs[i] = fullwave.get(i);
        }     
        Fitter fitter = new MarquardtFitter(gaussianModel);
        fitter.setData(xs, zs);
        
        return fitter;
    }
	
	// Perspective projection
	public Vertex projectPoint(Vector3D point, Vector3D camera, double[] angles) {
		Vector3D a = point;
		Vector3D c = camera;
		Vector3D e = a;
		
		// Camera transform
		Vector3D ac = a.subtract(c);
		Rotation rotation = new Rotation(RotationOrder.XYZ, angles[0], angles[1], angles[2]);
		Vector3D d = rotation.applyTo(ac);		
		
		double frac = e.getZ() / d.getZ();
		double bx = frac * d.getX() - e.getX();
		double by = frac * d.getY() - e.getY();
		Vertex v = new Vertex();
		v.pos = new Vector3D(bx, by, 0);
		
		return v;
	}
	
	/* FIXME
	public double calcRealArea(Rotation rotation, double incidenceAngle, Vector3D beamDir, Vector3D point, Material material, ScenePart scenePart, Sphere footprint, double footprintArea) {
		
		Vector3D axis = beamDir.normalize();	
		System.out.println(incidenceAngle + " " + axis.toString());
		int count = 0;
		
		Triangle[] triangles2d = new Triangle[scenePart.mPrimitives.size()];	    
		
		for(Primitive prim : scenePart.mPrimitives) {	// Transform all the primitive triangles from 3D to 2D
		    	 
			Triangle triangle3d = (Triangle)prim;
			//System.out.println(triangle3d.toString());
			double prevArea = triangle3d.calcArea3D();	
			double prevArea2 = triangle3d.calcArea2D();	
	    	
	    	double[] angles = rotation.getAngles(RotationOrder.XYZ);
	    	//System.out.println("EULE rad " + angles[0] + " " + angles[1] + " " + angles[2]);
	    	//System.out.println("EULE deg " + angles[0] * 180 / Math.PI  + " " + angles[1] * 180 / Math.PI  + " " + angles[2] * 180 / Math.PI );
	    	
	    	Vertex a = projectPoint(prim.getVertices().get(0).pos, absoluteBeamOrigin, angles);
	    	Vertex b = projectPoint(prim.getVertices().get(1).pos, absoluteBeamOrigin, angles);
	    	Vertex c = projectPoint(prim.getVertices().get(2).pos, absoluteBeamOrigin, angles);
	    	
	    	Triangle triangle2d = new Triangle(a, b, c);
	    	triangles2d[count++] = triangle2d;
	    	double triangleArea = triangle2d.calcArea2D();	 	
	    	//System.out.println(triangle2d.toString());
			//System.out.println(material.definition + " " + prevArea * 10000 + " is now " + triangleArea * 10000 + " (cm2)" );	
	    }	
			
	    double area = 0;		
	    count = 0;
				
		// Find which 2d triangles intersects with the beam footprint 
		for (int i = 0; i < scenePart.mPrimitives.size() ;i++) {
			if(triangles2d[i].intersectsCircle(footprint)) {
				count++;
				area += triangles2d[i].calcArea2D(); // TODO: Do not assume 100% overlap if intersects
			}	
		}
			
		System.out.println("Intersected triangles: " + count  + " of " + scenePart.mPrimitives.size());
		System.out.println("Footprint area: " + footprintArea  + " Target area: " + area);
		
		return area;
	}
	*/
	
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
        Node scenePartsNode = (Node) rootNode.getChild("sceneparts");
        
		ArrayList<Double> fullandecho = new ArrayList<>(); //test
        
        Scene scene = detector.scanner.platform.scene;

		Vector3D beamDir = absoluteBeamAttitude.applyTo(Directions.forward);

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

		TreeMap<Double, Double> reflections = new TreeMap<>();

		// List to store all intersections. This is required later to reconstruct the associations between extracted points
		// and the scene objects that caused them:
		ArrayList<RaySceneIntersection> intersects = new ArrayList<>();

		double radiusStep_rad =  detector.scanner.FWF_settings.beamDivergence_rad / detector.scanner.FWF_settings.beamSampleQuality;

		// ######## BEGIN Outer loop over radius steps from beam center to outer edge ##############
		for (int radiusStep = 0; radiusStep < detector.scanner.FWF_settings.beamSampleQuality; radiusStep++) {

			double subrayDivergenceAngle_rad = radiusStep * radiusStep_rad;

			// Rotate subbeam into divergence step (towards outer rim of the beam cone):
			Rotation r1 = new Rotation(Directions.right, subrayDivergenceAngle_rad);

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
				Rotation r2 = new Rotation(Directions.forward, circleStep_rad * circleStep).applyTo(r1);

				Vector3D subrayDirection = absoluteBeamAttitude.applyTo(r2).applyTo(Directions.forward);

				//RaySceneIntersection intersect = scene.getIntersection(absoluteBeamOrigin, subrayDirection, false);
                
                ///// JMONKEY /////       
                //System.out.println(absoluteBeamOrigin.getX() + " " + absoluteBeamOrigin.getY() + " " + absoluteBeamOrigin.getZ());
                //System.out.println(subrayDirection.getX() + " " + subrayDirection.getY() + " " + subrayDirection.getZ());
                CollisionResults results = new CollisionResults();
                Vector3f from = new Vector3f((float) absoluteBeamOrigin.getX(), (float) absoluteBeamOrigin.getZ(), (float) absoluteBeamOrigin.getY());
                Vector3f dir = new Vector3f((float) subrayDirection.getX(), (float) subrayDirection.getZ(), (float) subrayDirection.getY());
                Ray ray = new Ray(from, dir);          
                scenePartsNode.collideWith(ray, results);
                //System.out.println("Number of Collisions between" + scenePartsNode.getName()+ " and " + ray.getName() + ": " + results.size());
                //System.out.println("Number of Collisions between" + scenePartsNode.getName()+ " and : " + results.size());
                
                //Geometry geom;
                double jdist = 0; 
                Vector3f jnormal = null;
                for (int i = 0; i < results.size(); i++) {
                    // For each “hit”, we know distance, impact point, geometry.
                    float dist = results.getCollision(i).getDistance();   
                    //Vector3f pt = results.getCollision(i).getContactPoint();
                    Vector3f normal = results.getCollision(i).getContactNormal();
                    //geom = results.getCollision(i).getGeometry();
                    String target = results.getCollision(i).getGeometry().getName();
                    //System.out.println("Selection #" + i + ": " + target + " at " + pt + ", " + dist + " WU away.");
                    
                    jdist = dist;
                    jnormal = normal;
                }
               
                if (results.size() > 0) {
                
                // Tests
                //double cdist = intersect.point.distance(absoluteBeamOrigin);
                //double cangle = intersect.prim.getIncidenceAngle_rad(absoluteBeamOrigin, subrayDirection); 
                //double jangle = geom.get .getIncidenceAngle_rad(absoluteBeamOrigin, subrayDirection); 
                Vector3D jangle3d = new Vector3D(jnormal.getX(), jnormal.getY(), jnormal.getZ());
                Vector3D dir3d = new Vector3D(dir.getX(), dir.getY(), dir.getZ());
                double jangle = Math.PI - Vector3D.angle(jangle3d, dir3d);
                //System.out.println("Distances Custom " + cdist + " Monkey " + cdist);
                //System.out.println("Angles Custom " + cangle + " Monkey " + jangle);
                
                ///// JMONKEY /////   
                
              
					double incidenceAngle = jangle;
					double distance = jdist;					
					if(detector.cfg_device_rangeMin_m>distance) continue;
					double radius = Math.sin(subrayDivergenceAngle_rad) * distance;
					double targetArea = detector.scanner.calcFootprintArea(distance) / (double) detector.scanner.getNumRays(); 
					double intensity = calcIntensity(incidenceAngle, distance, 50, 0.1, targetArea, radius);
					reflections.put(distance, intensity);
                }
				
			}
			// ######## END Inner loop over sub-rays along the circle ##############
		}
                
				
//                if (intersect != null && intersect.prim != null) {
//
//					intersects.add(intersect);
//
//					// Incidence angle:
//					double incidenceAngle = intersect.prim.getIncidenceAngle_rad(absoluteBeamOrigin, subrayDirection);
//
//					// Distance between beam origin and intersection:
//					double distance = intersect.point.distance(absoluteBeamOrigin);
//                     //System.out.println("Selection classic at distance " + distance);
//					
//					if(detector.cfg_device_rangeMin_m>distance) continue;
//
//					// Distance between the beam's center line and the intersection point:
//					double radius = Math.sin(subrayDivergenceAngle_rad) * distance;
//
//					double targetArea = detector.scanner.calcFootprintArea(distance) / (double) detector.scanner.getNumRays(); 
//					
//					/* TODO Jorge: The real area computation is not working yet
//					  if(intersect.prim.material.classification != LasSpecification.GROUND && intersect.prim.material.spectra != null) {
//					 
//						Rotation beamRotation = absoluteBeamAttitude.applyTo(r2);
//						targetArea = calcRealArea(beamRotation, incidenceAngle, subrayDirection, intersect.point, intersect.prim.material, intersect.prim.part, new Sphere(intersect.point, detector.scanner.calcFootprintRadius(distance)), detector.scanner.calcFootprintArea(distance));	
//					}
//					*/
//					
//					double intensity = calcIntensity(incidenceAngle, distance, intersect.prim.material.reflectance, intersect.prim.material.specularity, targetArea, radius);
//
//					reflections.put(distance, intensity);
//				}
//			}
//			// ######## END Inner loop over sub-rays along the circle ##############
//		}
		// ######## END Outer loop over radius steps from beam center to outer edge ##############

		// ##################### END Perform raycasting for each sub-ray and find all intersections #################
	
		final double maxHitDist_m = reflections.lastKey();
		final double minHitDist_m = reflections.firstKey();	
        if (maxHitDist_m < 0) { // Abort if nothing was hit
			detector.scanner.setLastPulseWasHit(false);
			return;
		}		

		final double maxHitTime_ns = maxHitDist_m / cfg_speedOfLight_mPerNanosec + detector.scanner.FWF_settings.pulseLength_ns;	
		final double minHitTime_ns = minHitDist_m / cfg_speedOfLight_mPerNanosec - detector.scanner.FWF_settings.pulseLength_ns;
        final double totalHitTime_ns = maxHitTime_ns - minHitTime_ns;
        if(cfg_device_timeMin_ns > minHitTime_ns) {
			return;
		}
        
        ArrayList<Double> fullwave = calcFullWaveform(reflections, maxHitTime_ns, minHitTime_ns, totalHitTime_ns);
 
		Fitter fitter = initGaussianFitter(fullwave);
		
        // Perform peak detection
        final double eps = 0.001;
        final double minEchoWidth = 0.1;  //  min_wave_width
        final int cfg_numFullwaveBins = detector.scanner.FWF_settings.numFullwaveBins;
        final int win_size = detector.scanner.FWF_settings.winSize; // search for peaks around [-win_size, win_size]		
        int num_returns = 0;
        ArrayList<Measurement> PointsMeasurement=new ArrayList<>(); // temp solution
  
		for (int i = 0; i < fullwave.size(); ++i) {
			if(fullwave.get(i) < eps) continue;
			
			// peak detection
			boolean hasPeak = true;
			for (int j = Math.max(0, i - 1); j > Math.max(0, i - win_size); j--) {
				if (fullwave.get(j) < eps || fullwave.get(j) >= fullwave.get(i)) {
					hasPeak = false;
					break;
				}
			}
			if (hasPeak) {
				for (int j = Math.min(fullwave.size(), i + 1); j < Math.min(fullwave.size(), i + win_size); j++) {
					if (fullwave.get(j) < eps || fullwave.get(j) >= fullwave.get(i)) {
						hasPeak = false;
						break;
					}
				}
			}

			if (hasPeak) {
		        fitter.setParameters(new double[]{0, fullwave.get(i), i, 1});
		        try {
		        	fitter.fitData();
		        } catch (java.lang.RuntimeException e) { 
					continue;
				}
		        double echo_width = (double) fitter.getParameters()[3];
		        echo_width = (echo_width /  (double) cfg_numFullwaveBins) * totalHitTime_ns;
				if (echo_width < minEchoWidth )  {  
					continue;
				}
					
				// ########## END echo location, intensity and width extraction via Gaussian decomposition ###########
				double distance = cfg_speedOfLight_mPerNanosec * ((i / (double) cfg_numFullwaveBins) * totalHitTime_ns + minHitTime_ns);

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
				int classification = 0;
				if (closestIntersection != null) {
					//objects.add(closestIntersection.prim.part.id);
					hitObject = closestIntersection.prim.part.mId;
					classification =  closestIntersection.prim.material.classification;
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
				tmp.classification = classification;
				PointsMeasurement.add(tmp);
                fullandecho.add(echo_width); fullandecho.add(fullwave.get(i));
				++num_returns;
			}
		}
//    System.out.println(fullandecho);
//    if(fullandecho.get(0) == 1.1494054793226347 || fullandecho.get(0) == 1.143578712596994 && fullandecho.get(1) == 24.799861601181746 || fullandecho.get(1) == 44.1971145494791)
//        System.out.println("TRUE");
//    else
//        System.out.println("FALSE");
//    System.exit(1);

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
