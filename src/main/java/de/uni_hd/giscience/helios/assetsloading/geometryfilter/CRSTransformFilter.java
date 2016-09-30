package de.uni_hd.giscience.helios.assetsloading.geometryfilter;

import java.util.HashSet;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import de.uni_hd.giscience.helios.assetsloading.ScenePart;
import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;
import de.uni_hd.giscience.helios.core.scene.primitives.Triangle;
import de.uni_hd.giscience.helios.core.scene.primitives.Vertex;

public class CRSTransformFilter extends AbstractGeometryFilter {

	CoordinateReferenceSystem targetCRS = null;
	CoordinateReferenceSystem sourceCRS = null;

	public CRSTransformFilter(ScenePart parts, CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) {

		this.primsOut = parts;

		this.targetCRS = targetCRS;
		this.sourceCRS = sourceCRS;

	}

	@Override
	public ScenePart run() {

		if (CRS.equalsIgnoreMetadata(this.sourceCRS, this.targetCRS)) {
			System.out.println("Source CRS appears to equal target CRS. No Transformation required.");
			return this.primsOut;
		}

		// LidarSim.log.warning("Source CRS (" + this.sourceCRS.getName() + ") appears to be different from target CRS (" + this.targetCRS.getName()
		// + "). Will attempt to transform coordinates.");

		boolean lenient = true;
		MathTransform mathTransform = null;

		try {
			mathTransform = CRS.findMathTransform(sourceCRS, targetCRS, lenient);
			System.out.println("Transformation found!");

		} catch (FactoryException ex) {
			// java.util.logging.Logger.getLogger(Layer.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
			System.out.println("ERROR: Failed to find transformation from part CRS to survey CRS.");
			return this.primsOut;
		}

		if (mathTransform != null) {

			HashSet<Vertex> allVerts = primsOut.getAllVertices();

			for (Vertex v : allVerts) {

				DirectPosition2D pos_part = new DirectPosition2D(v.pos.getX(), v.pos.getY());

				double z = v.pos.getZ();

				DirectPosition2D pos_survey = new DirectPosition2D();

				try {
					mathTransform.transform(pos_part, pos_survey);
					v.pos = new Vector3D(pos_survey.x, pos_survey.y, z);
				} catch (Exception e) {
					// logger.log(Level.SEVERE, (Object) ((Throwable) ex).getMessage());
					System.out.println("Exception during transformation: " + e.getMessage());
				}

			}

			// Update triangle normals:
			for (Primitive p : primsOut.mPrimitives) {
				Triangle t = (Triangle) p;
				t.update();
			}

			try {

				primsOut.mEnv = primsOut.mEnv.transform(targetCRS, true);
			} catch (TransformException e) {

				e.printStackTrace();
			} catch (FactoryException e) {

				e.printStackTrace();
			}

		}

		else {
			System.out.println("Part CRS and survey CRS appear to be equal -> No transformation required.");
		}
		return primsOut;
	}

}
