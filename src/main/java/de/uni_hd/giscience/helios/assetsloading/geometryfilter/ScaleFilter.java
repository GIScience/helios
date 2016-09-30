
package de.uni_hd.giscience.helios.assetsloading.geometryfilter;

import de.uni_hd.giscience.helios.assetsloading.ScenePart;

public class ScaleFilter extends AbstractGeometryFilter {

	public ScaleFilter(ScenePart parts) {
		this.primsOut = parts;
	}

	@Override
	public ScenePart run() {

		if (primsOut == null) {
			return null;
		}

		Double scaleFactor = (Double) params.get("scale");

		if (scaleFactor != 0) {
			primsOut.mScale = scaleFactor;
		}

		return primsOut;
	}

}
