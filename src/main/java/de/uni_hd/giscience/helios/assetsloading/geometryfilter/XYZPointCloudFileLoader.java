package de.uni_hd.giscience.helios.assetsloading.geometryfilter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.vecmath.Color4f;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.assetsloading.ScenePart;
import de.uni_hd.giscience.helios.core.scene.Material;
import de.uni_hd.giscience.helios.core.scene.primitives.Voxel;

public class XYZPointCloudFileLoader extends AbstractGeometryFilter {

	// Logger log = null;
	Color4f color;

	String separator = " ";

	double voxelSize = 1;

	double maxColorValue = 0;

	public XYZPointCloudFileLoader(Color4f color) {

		this.color = color;
	}

	@Override
	public ScenePart run() {

		String filePathString = (String) params.get("filepath");

		// Read Separator:
		String pSep = (String) params.get("separator");

		if (pSep != null) {
			separator = pSep;
		}

		// Read Separator:
		Double pVoxelSize = (Double) params.get("voxelSize");

		if (pVoxelSize != null) {
			voxelSize = pVoxelSize;
		}

		// Read Max color Value:
		Double pMaxCol = (Double) params.get("maxColorValue");

		if (pMaxCol != null) {
			maxColorValue = pMaxCol;
		}

		String matName = "default";

		// log.info("Adding defalt material");
		Material mat = new Material();
		mat.useVertexColors = true;
		mat.isGround = true;
		mat.name = matName;

		materials.put(matName, mat);

		HashMap<Vector3D, Voxel> voxels = new HashMap<>();

		// log.info("Reading point cloud from XYZ file " + filePathString + " ...");

		String line = "";

		double roundFactor = 1.0 / voxelSize;

		int scale = 1;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePathString));

		} catch (Exception e) {
			// log.warning("Failed to open xyz point cloud file: " + filePathString);
			return null;
		}

		double minZ = Double.MAX_VALUE;
		double maxZ = Double.MIN_VALUE;

		try {
			while ((line = br.readLine()) != null) {

				line = line.trim();

				String[] lineParts = line.split(separator);

				// ########## BEGIN Read vertex position ##########
				if (lineParts.length >= 3) {

					double x = Double.parseDouble(lineParts[0]);
					double y = Double.parseDouble(lineParts[1]);
					double z = Double.parseDouble(lineParts[2]);

					if (z < minZ) {
						minZ = z;
					} else if (z > maxZ) {
						maxZ = z;
					}

					double vx = (((double) Math.round(x * roundFactor)) / roundFactor) * scale;
					double vy = (((double) Math.round(y * roundFactor)) / roundFactor) * scale;
					double vz = (((double) Math.round(z * roundFactor)) / roundFactor) * scale;

					Vector3D v = new Vector3D(vx, vy, vz);

					if (!voxels.containsKey(v)) {

						Voxel newVoxel = new Voxel(v, voxelSize);

						voxels.put(v, newVoxel);
					}

					Voxel vox = voxels.get(v);

					vox.numPoints++;

					if (lineParts.length >= 6) {

						double r = Double.parseDouble(lineParts[3]) / this.maxColorValue;
						double g = Double.parseDouble(lineParts[4]) / this.maxColorValue;
						double b = Double.parseDouble(lineParts[5]) / this.maxColorValue;

						vox.r += r;
						vox.g += g;
						vox.b += b;
					}
				}

			}
		} catch (Exception e) {
			// log.warning("Failed to read xyz point cloud file '" + filePathString + "': " + e.getMessage());
			return null;
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// log.info("done.");

		Iterator<Entry<Vector3D, Voxel>> iter = voxels.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Vector3D, Voxel> pair = (Map.Entry<Vector3D, Voxel>) iter.next();

			Voxel vox = pair.getValue();
			float r = (float) (vox.r / vox.numPoints);
			float g = (float) (vox.g / vox.numPoints);
			float b = (float) (vox.b / vox.numPoints);

			vox.v.color = new Color4f(r, g, b, 1);

			vox.material = getMaterial(matName);
			primsOut.mPrimitives.add(vox);

		}

		// log.info("Point cloud file read successful. # Voxels: " + primsOut.primitives.size());

		return primsOut;
	}
}
