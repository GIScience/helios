package de.uni_hd.giscience.helios.assetsloading.geometryfilter;

import java.util.HashMap;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import de.uni_hd.giscience.helios.assetsloading.ScenePart;
import de.uni_hd.giscience.helios.core.scene.Material;

public abstract class AbstractGeometryFilter {

	public CoordinateReferenceSystem sourceCRS;

	public HashMap<String, Object> params = new HashMap<>();
	public HashMap<String, Material> materials = new HashMap<>();

	protected ScenePart primsOut = new ScenePart();

	Material getMaterial(String materialName) {

		Material mat = materials.get(materialName);

		// TODO 5: Make it possible to load materials *after* the geometry?
		if (mat == null) {
			mat = new Material();
			mat.name = materialName;
			materials.put(materialName, mat);
		}

		return mat;
	}

	public abstract ScenePart run();

}
