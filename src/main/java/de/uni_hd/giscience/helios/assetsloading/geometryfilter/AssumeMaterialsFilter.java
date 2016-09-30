package de.uni_hd.giscience.helios.assetsloading.geometryfilter;

import javax.vecmath.Color4f;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.assetsloading.MaterialsFileReader;
import de.uni_hd.giscience.helios.assetsloading.ScenePart;
import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;
import de.uni_hd.giscience.helios.core.scene.primitives.Triangle;

public class AssumeMaterialsFilter extends AbstractGeometryFilter {

	// TODO 5: Pass sun direction from scene description
	Vector3D sunDir = new Vector3D(0, 0, 1);

	ScenePart primsIn = null;

	public AssumeMaterialsFilter(ScenePart parts, Vector3D sunDir) {
		this.primsIn = parts;
		this.sunDir = sunDir;
	}

	@Override
	public ScenePart run() {

		materials.putAll(MaterialsFileReader.loadMaterials("sceneparts/assumeMaterialsFilter.mtl"));

		Vector3D up = new Vector3D(0, 0, 1);

		for (Primitive prim : primsIn.mPrimitives) {

			if (prim.getClass() == Triangle.class) {
				Triangle tri = (Triangle) prim;

				double elevation = tri.getCentroid().getZ();
				double angle = Vector3D.angle(up, tri.getFaceNormal()) * 180 / Math.PI;

				// ######### BEGIN Make landcover assumptions based on terrain elevation, slope, angle to sun etc. ##########

				double snowline_var = 0;
				double snowline = 0;
				double timberline = 0;
				double timber_min_slope = 0;
				double rock_min_slope = 0;

				try {
					snowline = (double) params.get("snowline");
					snowline_var = (double) params.get("snowline_var");
					timberline = (double) params.get("timberline");
					timber_min_slope = (double) params.get("timber_min_slope");
					rock_min_slope = (double) params.get("rock_min_slope");
				} catch (Exception e) {

					System.out.println(e.getMessage());
				}

				// Initialize everything with grass:
				String materialName = "grass";
				tri.setAllVertexColors(new Color4f(0.3f, 0.8f, 0.3f, 1f));

				// Forest:
				if (angle > timber_min_slope && elevation < timberline) {
					materialName = "forest";
					tri.setAllVertexColors(new Color4f(0.1f, 0.3f, 0.1f, 1f));

				}

				// Snow:
				double sunAngle = Vector3D.angle(sunDir.negate(), tri.getFaceNormal());
				double temperature = snowline - elevation - snowline_var * sunAngle;

				if (temperature < 0) {
					materialName = "snow";
					tri.setAllVertexColors(new Color4f(1, 1, 1, 1));
				}

				// Rock:
				if (angle > rock_min_slope) {
					materialName = "rock";
					tri.setAllVertexColors(new Color4f(0.6f, 0.6f, 0.6f, 1));
				}
				// ######### END Make landcover assumptions based on terrain elevation, slope, angle to sun etc. ##########

				tri.material = getMaterial(materialName);
			}
		}

		return primsIn;
	}

}
