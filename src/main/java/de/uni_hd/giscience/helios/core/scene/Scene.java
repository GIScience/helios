package de.uni_hd.giscience.helios.core.scene;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import javax.vecmath.Color4f;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.Asset;
import de.uni_hd.giscience.helios.core.scene.primitives.AABB;
import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;
import de.uni_hd.giscience.helios.core.scene.primitives.Vertex;

public class Scene extends Asset implements Serializable {

	private static final long serialVersionUID = -2223398133296904729L;

	public ArrayList<Primitive> primitives = new ArrayList<>();

	private KDTreeNode kdtree = null;

	private AABB bbox = null;
	private AABB bbox_crs = null;

	// ########## BEGIN Visual stuff ###########
	public Vector3D sunDir = new Vector3D(1, 1, -1);

	public Color4f skyColor = null;
	public String skyboxTexturesFolder = "";
	public float skyboxAzimuth_rad = 0;
	// ########## END Visual stuff ###########

	public boolean finalizeLoading() {

		if (primitives.size() == 0) {
			return false;
		}

		// ################ BEGIN Shift primitives to origin ##################

		// Translate scene coordinates to origin (to prevent wasting of floating point precision):

		HashSet<Vertex> vertices = new HashSet<>();

		// Collect all vertices in a HashSet (makes sure that we don't translate the same vertex multiple times):
		for (Primitive p : primitives) {

			ArrayList<Vertex> verts = p.getVertices();

			if (verts != null) {
				vertices.addAll(verts);
			}
		}

		if (vertices.size() == 0) {
			return false;
		}

		// LidarSim.log.info("Total # of primitives in scene: " + primitives.size() + "\n");

		// ########## BEGIN Move the scene so that bounding box minimum is (0,0,0) ########
		// This is done to prevent precision problems (e.g. camera jitter)

		// Store original bounding box (CRS coordinates):
		this.bbox_crs = AABB.getForVertices(vertices);

		Vector3D diff = this.bbox_crs.min;

		/*
		 * System.out.println("CRS bounding box (by vertices): " + this.bbox_crs.toString()); System.out.println("Shift: " + diff); System.out.println("# vertices to translate: " +
		 * vertices.size());
		 */

		// Iterate over the hash set and translate each vertex:
		for (Vertex v : vertices) {
			v.pos = v.pos.subtract(diff);
		}

		for (Primitive p : primitives) {
			p.update();
		}

		// Get new bounding box of tranlated scene:
		this.bbox = AABB.getForVertices(vertices);

		// System.out.println("Actual bounding box (by vertices): " + this.bbox.toString());

		// ################ END Shift primitives to origin ##################

		// ############# BEGIN Build KD-tree ##################
		System.out.println("Building KD-Tree... ");

		long timeStart = System.nanoTime();

		kdtree = KDTreeNode.build(primitives);

		long timeFinish = System.nanoTime();

		double seconds = (double) (timeFinish - timeStart) / 1000000000;

		System.out.println("finished in " + seconds + " sec.");
		// LidarSim.log.info("Max KD tree depth: " + kdtree.stats_maxDepthReached);
		// LidarSim.log.info("Max # primitives in KD tree leaf: " + kdtree.stats_maxNumPrimsInLeaf);
		// ############# END Build KD-tree ##################

		return true;
	}

	public AABB getAABB() {
		return this.bbox;
	}

	public Vector3D getGroundPointAt(Vector3D point) {

		Vector3D origin = new Vector3D(point.getX(), point.getY(), 100000);
		Vector3D dir = new Vector3D(0, 0, -1);

		RaySceneIntersection intersect = getIntersection(origin, dir, true);

		if (intersect == null) {
			return null;
		}

		return intersect.point;
	}

	public RaySceneIntersection getIntersection(Vector3D rayOrigin, Vector3D rayDir, boolean groundOnly) {

		double[] tMinMax = bbox.getRayIntersection(rayOrigin, rayDir);

		// if tMinMax == null, it doesn't. In this case, we can abort here:
		if (tMinMax == null) {
			return null;
		}

		boolean bruteForce = false;

		RaySceneIntersection result = null;

		if (!bruteForce) {

			KdTreeRaycaster raycaster = new KdTreeRaycaster(kdtree);

			result = raycaster.search(rayOrigin, rayDir, tMinMax[0], tMinMax[1], groundOnly);
		} else {
			double minDist = Double.MAX_VALUE;

			for (Primitive p : this.primitives) {
				double[] hitDist = p.getRayIntersection(rayOrigin, rayDir);

				if ((hitDist[0] >= 0 && hitDist[0] < minDist)) {
					minDist = hitDist[0];

					result = new RaySceneIntersection();
					result.prim = p;
					result.point = rayOrigin.add(rayDir.scalarMultiply(minDist));
				}
			}
		}

		return result;
	}

	public TreeMap<Double, Primitive> getIntersections(Vector3D rayOrigin, Vector3D rayDir, boolean groundOnly) {

		double[] tMinMax = bbox.getRayIntersection(rayOrigin, rayDir);

		// if tMinMax == null, it doesn't. In this case, we can abort here:
		if (tMinMax == null) {
			return null;
		}

		KdTreeRaycaster raycaster = new KdTreeRaycaster(kdtree);
		return raycaster.searchAll(rayOrigin, rayDir, tMinMax[0], tMinMax[1], groundOnly);
	}

	public Vector3D getShift() {
		return this.bbox_crs.min;
	}
	
	public void writeObject(String path) {	
		   
		System.out.println("Writing " + path + "...");	
	    	
		try {			
			FileOutputStream fos = new FileOutputStream(path);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
		     
		    oos.writeObject(this);
		   
		    oos.close();
		    fos.close(); 		    
		} catch (IOException e) {			
			e.printStackTrace();
		}	      
	}
	
	public static Scene readObject(String path) {
		
		Scene scene = null;
		System.out.println("Reading " + path + "...");		
		
		try {			
			FileInputStream fis = new FileInputStream(path);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream ois = new ObjectInputStream(bis);
	        
			scene = (Scene)ois.readObject();
			
			ois.close();
			fis.close(); 
		} catch (IOException | ClassNotFoundException e) {			
			e.printStackTrace();
		}
		
		return scene;
	}
}
