package de.uni_hd.giscience.helios.assetsloading.geometryfilter;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;

import de.uni_hd.giscience.helios.assetsloading.ScenePart;

public class RotateFilter extends AbstractGeometryFilter {

	public RotateFilter(ScenePart parts) {
		this.primsOut = parts;
	}

	@Override
	public ScenePart run() {

		if (primsOut == null) {
			return null;
		}

		Double x = (Double) params.get("x");
		Double y = (Double) params.get("y");
		Double z = (Double) params.get("z");

		if (x == null) {
			x = (double) 0;
		}
		if (y == null) {
			y = (double) 0;
		}
		if (z == null) {
			z = (double) 0;
		}

		
		Rotation rotation = (Rotation) params.get("rotation");

		if (rotation != null) {
		
			primsOut.mRotation = rotation.applyTo(primsOut.mRotation);
			
			/*
			HashSet<Vertex> allVerts = primsOut.getAllVertices();

			
			for (Vertex v : allVerts) {
				v.pos = rotation.applyTo(v.pos);

				if (v.normal != null) {
					v.normal = rotation.applyTo(v.normal);
				}
			}
			*/
		} 

		return primsOut;
	}

}
