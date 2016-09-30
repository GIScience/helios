package de.uni_hd.giscience.helios.assetsloading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;
import de.uni_hd.giscience.helios.core.scene.primitives.Triangle;
import de.uni_hd.giscience.helios.core.scene.primitives.Vertex;

public class ScenePart {
	public ArrayList<Primitive> mPrimitives = new ArrayList<>();
	public String mId = "";
	
	public Vector3D mOrigin = new Vector3D(0,0,0);
	public Rotation mRotation = new Rotation(new Vector3D(1,0,0),0);
	public double mScale = 1;
	
	// GeoTools members:
	public CoordinateReferenceSystem mCrs = null;
	public ReferencedEnvelope mEnv;

	
	public HashSet<Vertex> getAllVertices() {

		HashSet<Vertex> allPos = new HashSet<>();

		for (Primitive p : mPrimitives) {
			Triangle t = (Triangle) p;

			for (int ii = 0; ii <= 2; ii++) {
				allPos.add(t.verts[ii]);
			}
		}

		return allPos;
	}

		
	
	public void smoothVertexNormals() {

		HashMap<Vertex, ArrayList<Triangle>> hm = new HashMap<>();

		// ######### BEGIN Build a map of "Vertex -> Triangles that have this vertex as corner point" #########

		for (Primitive prim : mPrimitives) {
			Triangle t = (Triangle) prim;

			for (int ii = 0; ii <= 2; ii++) {
				if (!hm.containsKey(t.verts[ii])) {
					hm.put(t.verts[ii], new ArrayList<Triangle>());
				}

				hm.get(t.verts[ii]).add(t);
			}
		}
		// ######### END Build a map of "Vertex -> Triangles that have this vertex as corner point" #########

		// ############# BEGIN Iterate over all vertices of the part and set the normal of each vertex to the mean normal of all adjacent triangles ###########
		Iterator<Map.Entry<Vertex, ArrayList<Triangle>>> iter = hm.entrySet().iterator();

		while (iter.hasNext()) {
			Entry<Vertex, ArrayList<Triangle>> pair = (Map.Entry<Vertex, ArrayList<Triangle>>) iter.next();

			Vector3D normal = new Vector3D(0, 0, 0);

			// System.out.println("# Adjacent faces: " + pair.getValue().size());
			for (Triangle t : pair.getValue()) {
				normal = normal.add(t.getFaceNormal());
			}

			if (normal.getNorm() > 0) {
				normal = normal.normalize();
				pair.getKey().normal = normal;
			}

		}
		// ############# END Iterate over all vertices of the part and set the normal of each vertex to the mean normal of all adjacent triangles ###########

	}

}
