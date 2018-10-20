package de.uni_hd.giscience.helios.core.scanner.detector;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.LasSpecification;
import de.uni_hd.giscience.helios.core.scene.RaySceneIntersection;
import de.uni_hd.giscience.helios.core.scene.Scene;

public class SingleRayPulseRunnable extends AbstractPulseRunnable {

	public SingleRayPulseRunnable(AbstractDetector detector, Vector3D absoluteBeamOrigin, Rotation absoluteBeamAttitude, int pulseNumber, Long gpstime, Node rootNode) {
		super(detector, absoluteBeamOrigin, absoluteBeamAttitude, pulseNumber, gpstime, rootNode);
	}

	@Override
	public void run() {
 Node scenePartsNode = (Node) rootNode.getChild("sceneparts");
		Scene scene = detector.scanner.platform.scene;

		Vector3D beamDir = absoluteBeamAttitude.applyTo(Directions.forward);

		// Early abort if central axis of the beam does not intersect with the scene:
		double[] tMinMax = scene.getAABB().getRayIntersection(absoluteBeamOrigin, beamDir);

		if (tMinMax == null) {
			detector.scanner.setLastPulseWasHit(false);
			return;
		}
        
        ///// JMONKEY /////       
                //System.out.println(absoluteBeamOrigin.getX() + " " + absoluteBeamOrigin.getY() + " " + absoluteBeamOrigin.getZ());
                //System.out.println(subrayDirection.getX() + " " + subrayDirection.getY() + " " + subrayDirection.getZ());
                CollisionResults results = new CollisionResults();
                Vector3f from = new Vector3f((float) absoluteBeamOrigin.getX(), (float) absoluteBeamOrigin.getZ(), (float) absoluteBeamOrigin.getY());
                Vector3f dir = new Vector3f((float) beamDir.getX(), (float) beamDir.getZ(), (float) beamDir.getY());
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
                double targetArea = detector.scanner.calcFootprintArea(distance); 
                double intensity = calcIntensity(incidenceAngle, distance, 50, 0.1, targetArea);
					
               
				
	

//		RaySceneIntersection intersect = scene.getIntersection(absoluteBeamOrigin, beamDir, false);
//
//		if (intersect == null || intersect.point == null || intersect.prim.material.classification == LasSpecification.WATER) {  // TODO: Deal with water do not just ignore it
//			detector.scanner.setLastPulseWasHit(false);
//			return;
//		}
//
//		detector.scanner.setLastPulseWasHit(true);
//		
//		double distance = intersect.point.distance(absoluteBeamOrigin);
//		
//		double incidenceAngle = intersect.prim.getIncidenceAngle_rad(absoluteBeamOrigin, beamDir);		
//		
//		double targetArea = detector.scanner.calcFootprintArea(distance); 
//		
//		double intensity = calcIntensity(incidenceAngle, distance, intersect.prim.material.reflectance, intersect.prim.material.specularity, targetArea);

		capturePoint(absoluteBeamOrigin, beamDir, distance, intensity, 0, 0, currentPulseNum, 0, "nul", 0);
	}

    }}
