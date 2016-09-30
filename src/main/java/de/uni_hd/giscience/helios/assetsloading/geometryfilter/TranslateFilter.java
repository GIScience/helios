package de.uni_hd.giscience.helios.assetsloading.geometryfilter;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.assetsloading.ScenePart;

public class TranslateFilter extends AbstractGeometryFilter {

	public TranslateFilter(ScenePart parts) {
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

		

		Vector3D offset = (Vector3D) params.get("offset");

		if (offset != null) {
			
			primsOut.mOrigin = offset;
		} 

		return primsOut;
	}

}
