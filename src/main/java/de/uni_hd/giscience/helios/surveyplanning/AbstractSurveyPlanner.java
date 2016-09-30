// TODO 5: Add optional platform and scanner constraints

package de.uni_hd.giscience.helios.surveyplanning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.platform.Platform;
import de.uni_hd.giscience.helios.core.scanner.Scanner;
import de.uni_hd.giscience.helios.core.scene.RaySceneIntersection;
import de.uni_hd.giscience.helios.core.scene.Scene;
import de.uni_hd.giscience.helios.core.scene.primitives.AABB;
import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;

public abstract class AbstractSurveyPlanner {

	Scene scene = null;
	Platform platform = null;
	Scanner scanner = null;

	String id = "Unnamed";

	double cfg_minCoverage_percent = 50;
	double cfg_hitTolerance_m = 0.1;
	boolean cfg_interactive = false;
	double cfg_gridSizeVTP_m = 50;
	double cfg_gridSizePSP_m = 50;
	String cfg_pspRemoveOrder = "viewshedSize";

	String outputPath = "output/Survey Planning";

	HashSet<Vector3D> vtps = new HashSet<>();
	ArrayList<ScanPosition> psps = new ArrayList<>();
	HashSet<ScanPosition> selectedScanPositions = new HashSet<>();

	Vector3D lastAddedTestPoint = new Vector3D(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

	AABB roi = null;
	Vector3D roi_min = new Vector3D(25, 25, 0);
	Vector3D roi_max = new Vector3D(45, 45, 50);

	int numTestPointsTotal = 0;
	int numPotentialScanPositionsTotal = 0;

	void addTestPoint(Vector3D rayOrigin, Vector3D rayDir) {

		TreeMap<Double, Primitive> hitPrims = scene.getIntersections(rayOrigin, rayDir, false);

		if (hitPrims == null) {
			return;
		}

		Iterator<Entry<Double, Primitive>> iter = hitPrims.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Double, Primitive> entry = (Entry<Double, Primitive>) iter.next();

			double hitDistance = entry.getKey();

			Vector3D hitPos = rayOrigin.add(rayDir.scalarMultiply(hitDistance));

			if (hitPos.distance(lastAddedTestPoint) >= cfg_gridSizeVTP_m || true) {
				vtps.add(hitPos);
				lastAddedTestPoint = hitPos;
			}
		}
	}

	public void computeViewsheds() {
		// ############ BEGIN Compute viewshed for all potential scan positions ############

		System.out.println("Computing viewsheds for all potential scan positions...");

		long timeStart = System.nanoTime();

		int positionsChecked = 0;
		int lastPercent = -1;

		for (ScanPosition psp : psps) {

			psp.visiblePoints.clear();

			if (platform == null) {
				System.out.println("Platform is null!");
			}

			Vector3D rayOrigin = psp.pos.add(platform.cfg_device_relativeMountPosition);

			HashSet<VisiblePoint> vsps = getVisiblePoints(rayOrigin);

			psp.visiblePoints.clear();

			double angleSum = 0;

			for (VisiblePoint v : vsps) {

				// if (v.angle > 1 * (Math.PI / 180.0)) {
				psp.visiblePoints.add(v.pos);
				angleSum += v.angle;
				// }
			}

			// Store arithmetic mean of incidence angles:
			psp.avgIncidenceAngle_rad = angleSum / vsps.size();

			// ################## BEGIN Print status (percentage complete) ################
			positionsChecked++;

			int percent = (int) Math.floor(((float) positionsChecked / (float) psps.size()) * 100);

			if (percent % 10 == 0 && percent != lastPercent) {
				System.out.print(percent + "%...");
				lastPercent = percent;
			}
			// ################## END Print status (percentage complete) ################
		}

		System.out.println();

		long timeFinish = System.nanoTime();
		double seconds = (double) (timeFinish - timeStart) / 1000000000;

		System.out.println("finished in " + seconds + " seconds. # Test points: " + vtps.size());

		// ############ END Compute viewshed for all potential scan positions ############

	}

	HashSet<VisiblePoint> getVisiblePoints(Vector3D rayOrigin) {

		HashSet<VisiblePoint> result = new HashSet<>();

		for (Vector3D testPoint : vtps) {

			Vector3D rayDir = testPoint.subtract(rayOrigin);

			if (rayDir.getNorm() == 0) {
				continue;
			}

			rayDir = rayDir.normalize();

			// Otherwise, check for intersection with a primitive using the kdtree:
			RaySceneIntersection intersect = scene.getIntersection(rayOrigin, rayDir, false);

			// TODO 4: Try to get true incidence angle
			double incidenceAngle = (Math.PI / 4);

			double intersectDistance = Double.MAX_VALUE;

			if (intersect != null) {
				intersectDistance = rayOrigin.distance(intersect.point);
				incidenceAngle = intersect.prim.getIncidenceAngle_rad(rayOrigin, rayDir);
			}

			double testpointRayDistance = Vector3D.crossProduct(rayDir, testPoint.subtract(rayOrigin)).getNorm();

			if (testpointRayDistance < cfg_hitTolerance_m && rayOrigin.distance(testPoint) <= intersectDistance + cfg_hitTolerance_m) {

				VisiblePoint rsc = new VisiblePoint();
				rsc.angle = incidenceAngle;
				rsc.pos = testPoint;

				result.add(rsc);
			}
		}

		return result;
	}

	public void init() {

		if (scene == null) {
			// LidarSim.log.severe("Scene is null!");
			return;
		}

		// ######### BEGIN Create output folder #########
		Calendar cal = Calendar.getInstance();
		cal.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String date = sdf.format(cal.getTime());

		// TODO 5: Don't hardcode this
		outputPath = "output/" + File.separator + "Survey Planning" + File.separator + scene.name + File.separator + date + File.separator;

		// Create output folder:
		File outputFilePath = new File(outputPath);
		outputFilePath.mkdirs();

		setUpVTPs();

		setUpPSPs();

		computeViewsheds();
	}

	void printSelectedPositionsInfo() {

		PrintWriter bestPositionsFileWriter = null;

		try {
			bestPositionsFileWriter = new PrintWriter(outputPath + File.separator + "selected_positions.xyz");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// ############# BEGIN Print info about selected scan positions ##############

		String formatString = "%03d";

		int ii = 0;

		HashSet<Vector3D> coveredPoints = new HashSet<>();

		for (ScanPosition stp : selectedScanPositions) {

			coveredPoints.addAll(stp.visiblePoints);

			PrintWriter coverageFileWriter = null;
			try {
				coverageFileWriter = new PrintWriter(outputPath + File.separator + "scan" + String.format(formatString, ii) + "_visibility_analysis.xyz");

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			Vector3D shiftedScanPos = stp.pos.add(scene.getShift());

			int randR = (int) (Math.random() * 255);
			int randG = (int) (Math.random() * 255);
			int randB = (int) (Math.random() * 255);

			String randomColor = " " + randR + " " + randG + " " + randB;

			// Write scan position itself:
			bestPositionsFileWriter.println(shiftedScanPos.getX() + " " + shiftedScanPos.getY() + " " + shiftedScanPos.getZ() + randomColor);

			// Write points that are exclusively visible from this scan position:
			for (Vector3D v : stp.visiblePoints) {

				String color = " 0 0 255";

				Vector3D shifted = v.add(scene.getShift());

				if (stp.exclusivePoints.contains(v)) {
					color = randomColor;
				}

				coverageFileWriter.println(shifted.getX() + " " + shifted.getY() + " " + shifted.getZ() + " " + color);
			}

			coverageFileWriter.close();

			ii++;
		}
		// ############# END Print info about selected scan positions ##############

		bestPositionsFileWriter.close();

		float coveragePercent = ((float) coveredPoints.size()) / vtps.size() * 100;
		System.out.println("Coverage: " + coveredPoints.size() + " of " + vtps.size() + " (" + coveragePercent + "%)");
	}

	void setUpPSPs() {

		psps.clear();

		// ################ BEGIN Set up scanner test positions ###################

		Vector3D rayDir = new Vector3D(0, 0, -1);

		double z = 10000;

		AABB bbox = scene.getAABB();

		for (double x = bbox.min.getX(); x <= bbox.max.getX(); x += cfg_gridSizePSP_m) {
			for (double y = bbox.min.getY(); y <= bbox.max.getY(); y += cfg_gridSizePSP_m) {

				Vector3D rayOrigin = new Vector3D(x, y, z);

				RaySceneIntersection intersect = scene.getIntersection(rayOrigin, rayDir, true);

				if (intersect == null) {
					continue;
				}

				ScanPosition stp = new ScanPosition();
				stp.pos = intersect.point;
				psps.add(stp);
			}
		}

		// LidarSim.log.info("# Scan test positions: " + potentialScanPositions.size());

		// ################ END Set up scanner test positions ###################

		// ############## BEGIN Write PSPs to file for debugging ##############
		PrintWriter pspFileWriter = null;

		try {
			pspFileWriter = new PrintWriter(outputPath + File.separator + "psps.xyz");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String formatString = "%.3f";

		for (ScanPosition v : psps) {

			// Un-Do scene translation for export:
			Vector3D vShifted = v.pos.add(scene.getShift());

			String sx = String.format(formatString, vShifted.getX());
			String sy = String.format(formatString, vShifted.getY());
			String sz = String.format(formatString, vShifted.getZ());

			pspFileWriter.println(sx + " " + sy + " " + sz);
		}

		pspFileWriter.close();
		// ############## END Write PSPs to file for debugging ##############

		numPotentialScanPositionsTotal = psps.size();
	}

	void setUpVTPs() {

		// NOTE: This offset is required to avoid VTPs being placed directly at the edge of a
		// rectangular ground plane, where test rays might miss it by going over the edge into nowhere due to numerical errors:
		// double offset = 0.1;

		vtps.clear();

		this.roi = scene.getAABB();

		// ################ BEGIN Set up test points #################
		// long timeStart = System.nanoTime();

		// LidarSim.log.info("Finding test points...");

		for (double y = roi.min.getY(); y <= roi.max.getY(); y += cfg_gridSizeVTP_m) {
			for (double z = roi.min.getZ(); z <= roi.max.getZ(); z += cfg_gridSizeVTP_m) {

				double x = -1000000;

				Vector3D rayOrigin = new Vector3D(x, y, z);
				Vector3D rayDir = new Vector3D(1, 0, 0);

				addTestPoint(rayOrigin, rayDir);
			}
		}

		for (double x = roi.min.getX(); x <= roi.max.getX(); x += cfg_gridSizeVTP_m) {
			for (double z = roi.min.getZ(); z <= roi.max.getZ(); z += cfg_gridSizeVTP_m) {

				double y = -1000000;

				Vector3D rayOrigin = new Vector3D(x, y, z);
				Vector3D rayDir = new Vector3D(0, 1, 0);

				addTestPoint(rayOrigin, rayDir);
			}
		}

		// Down from the sky:
		for (double x = roi.min.getX(); x <= roi.max.getX(); x += cfg_gridSizeVTP_m) {
			for (double y = roi.min.getY(); y <= roi.max.getY(); y += cfg_gridSizeVTP_m) {

				double z = -1000000;

				Vector3D rayOrigin = new Vector3D(x, y, z);
				Vector3D rayDir = new Vector3D(0, 0, 1);

				addTestPoint(rayOrigin, rayDir);
			}
		}

		// long timeFinish = System.nanoTime();
		// double seconds = (double) (timeFinish - timeStart) / 1000000000;
		// LidarSim.log.info("finished in " + seconds + " seconds. # Test points: " + testPoints.size());
		// ################ END Set up test points #################

		// ############## BEGIN Write test points to file for debugging ##############
		PrintWriter vtpFileWriter = null;
		try {
			vtpFileWriter = new PrintWriter(outputPath + File.separator + "vtps.xyz");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String formatString = "%.3f";

		for (Vector3D v : vtps) {

			// Un-Do scene translation for export:
			Vector3D vShifted = v.add(scene.getShift());

			String sx = String.format(formatString, vShifted.getX());
			String sy = String.format(formatString, vShifted.getY());
			String sz = String.format(formatString, vShifted.getZ());

			vtpFileWriter.println(sx + " " + sy + " " + sz);
		}

		vtpFileWriter.close();
		// ############## END Write test points to file for debugging ##############

		numTestPointsTotal = vtps.size();
	}

	public void writeSurveyFile() {

		// ################## BEGIN Write survey file ###################

		String platformString = platform.sourceFilePath + "#" + platform.id;
		String scannerString = scanner.sourceFilePath + "#" + scanner.id;
		String sceneString = scene.sourceFilePath + "#" + scene.id;

		PrintWriter sfw = null;

		try {
			sfw = new PrintWriter(outputPath + File.separator + "survey.xml");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		sfw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sfw.println("<document>");

		String surveyName = scene.name + " - optimal survey";

		sfw.println("<survey name=\"" + surveyName + "\" scene=\"" + sceneString + "\">");

		// ############## BEGIN Write scans ###########
		for (ScanPosition sp : selectedScanPositions) {

			Vector3D shifted = sp.pos.add(scene.getShift());

			// ############ BEGIN Write position to survey file ##############
			sfw.println("<scan platform=\"" + platformString + "\" scanner=\"" + scannerString + "\">");
			sfw.println("<scannerRotation axis=\"yaw\" angle=\"0.001\" start=\"0\" end=\"360\"/>");
			sfw.println("<leg x=\"" + shifted.getX() + "\" y=\"" + shifted.getY() + "\" z=\"" + shifted.getZ() + "\"/>");
			sfw.println("</scan>");
			sfw.flush();
			// ############ END Write position to survey file ##############
		}
		// ############## END Write scans ###########

		sfw.println("</survey>");
		sfw.println("</document>");

		sfw.close();
		// ################## END Write survey file ###################
	}

	public abstract void run();

}
