package de.uni_hd.giscience.helios.assetsloading.geometryfilter;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.DataSourceException;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.geometry.Envelope;

import de.uni_hd.giscience.helios.assetsloading.ScenePart;
import de.uni_hd.giscience.helios.core.scene.primitives.Triangle;
import de.uni_hd.giscience.helios.core.scene.primitives.Vertex;

public class GeoTiffFileLoader extends AbstractGeometryFilter {

	public ScenePart run() {

		String filePathString = (String) params.get("filepath");

		// LidarSim.log.info("Reading 3D model from GeoTiff file '" + filePathString + "'...");

		File file = new File(filePathString);

		if (!file.exists()) {
			// LidarSim.log.severe("File not found. Aborting load attempt.");
			System.exit(1);
		}

		// materials.putAll(MaterialsFileReader.loadMaterials(filePathString + ".mtl"));

		// AbstractGridFormat format = GridFormatFinder.findFormat(file);

		GeoTiffReader reader = null;
		try {

			reader = new GeoTiffReader(file, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));

			// TODO 4: Understand why logging on info level doesn't work any more after this point

		} catch (DataSourceException e) {
			e.printStackTrace();
		}

		GridCoverage2D coverage = null;
		try {
			coverage = (GridCoverage2D) reader.read(null);
		} catch (IOException e) {
			e.printStackTrace();
		}

		sourceCRS = coverage.getCoordinateReferenceSystem2D();

		Envelope env = coverage.getEnvelope();

		Raster raster = coverage.getRenderedImage().getData();

		Vertex[][] vertices = new Vertex[raster.getWidth()][raster.getHeight()];

		int rasterWidth = raster.getWidth();
		int rasterHeight = raster.getHeight();

		Envelope2D coverageEnv = coverage.getEnvelope2D();

		double minx = coverageEnv.x;
		double miny = coverageEnv.y;

		// log.warning("Min x: " + minx + ", Min y: " + miny);

		GridGeometry2D geometry = coverage.getGridGeometry();
		GridEnvelope2D gridEnv = geometry.getGridRange2D();

		double pixelWidth = coverageEnv.width / gridEnv.width;
		double pixelHeight = coverageEnv.height / gridEnv.height;

		boolean addHoles = false;

		// ########## BEGIN Fill array of vertices ##########
		for (int x = 0; x < rasterWidth; x++) {
			for (int y = 0; y < rasterHeight; y++) {

				Vertex v = new Vertex();

				double z = raster.getSampleFloat(x, y, 0);

				if (z < -100 && !addHoles) {
					vertices[x][y] = null;
				} else {
					v.pos = new Vector3D((minx + x * pixelWidth), (miny - y * pixelHeight) + coverageEnv.height, z);

					float tx = ((float) x) / rasterWidth;
					float ty = ((float) rasterHeight - y) / rasterHeight;
					v.texcoords = new Vector2D(tx, ty);

					vertices[x][y] = v;
				}
			}
		}
		// ########## END Fill array of vertices ##########

		// ########## BEGIN Create triangles #############
		for (int x = 0; x < rasterWidth - 1; x++) {
			for (int y = 0; y < rasterHeight - 1; y++) {

				Vertex vert0 = vertices[x][y];
				Vertex vert1 = vertices[x][y + 1];
				Vertex vert2 = vertices[x + 1][y + 1];
				Vertex vert3 = vertices[x + 1][y];

				if (vert0 != null && vert1 != null && vert3 != null) {
					Triangle tri1 = new Triangle(vert0, vert1, vert3);

					tri1.material = getMaterial("default");
					primsOut.mPrimitives.add(tri1);

				}

				if (vert1 != null && vert2 != null && vert3 != null) {

					Triangle tri2 = new Triangle(vert1, vert2, vert3);

					tri2.material = getMaterial("default");
					primsOut.mPrimitives.add(tri2);

				}
			}
		}
		// ########## END Create triangles #############

		primsOut.mEnv = new ReferencedEnvelope(env);

		// TODO 4: Understand what this exactly does. Especially why it changes lighting.
		primsOut.smoothVertexNormals();

		return primsOut;

	}
}
