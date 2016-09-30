package de.uni_hd.giscience.helios.surveyplanning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Scanner;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class SurveyPlanner1 extends AbstractSurveyPlanner {

	@Override
	public void run() {

		int scansCount = 1;

		Scanner keyboard = new Scanner(System.in);

		while (true) {

			ScanPosition bestPos = new ScanPosition();

			System.out.print("Scan " + scansCount + ": Searching best position... ");

			int positionsChecked = 0;
			int lastPercent = -1;

			int bestPosVisiblePoints = 0;

			for (ScanPosition stp : psps) {

				HashSet<Vector3D> visiblePoints = new HashSet<>();

				for (Vector3D v : stp.visiblePoints) {
					if (vtps.contains(v)) {
						visiblePoints.add(v);
					}
				}

				if (visiblePoints.size() > bestPosVisiblePoints) {
					bestPos = stp;
					bestPosVisiblePoints = visiblePoints.size();
				}

				positionsChecked++;

				int percent = (int) Math.floor(((float) positionsChecked / (float) psps.size()) * 100);

				if (percent % 10 == 0 && percent != lastPercent) {
					System.out.print(percent + "%...");
					lastPercent = percent;
				}
			}

			System.out.println();

			if (bestPosVisiblePoints == 0) {
				// LidarSim.log.info("No further test points can be reached. Aborting.");
				break;
			} else {

				// Vector3D bestPosShifted = bestPos.pos.add(scene.getShift());
				// LidarSim.log.info("Best position: " + bestPosShifted);

				// ################### BEGIN For each potential scan position, write results of visibility analysis to file ##################
				String formatString = "%03d";

				PrintWriter scanPositionsFileWriter = null;
				try {
					scanPositionsFileWriter = new PrintWriter(outputPath + File.separator + "scan" + String.format(formatString, scansCount) + "_visibility_analysis.xyz");

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

				for (ScanPosition stp : psps) {
					double percent = (float) stp.numVisiblePoints() / (float) vtps.size();

					// Un-Do scene translation for export:
					Vector3D shifted = stp.pos.add(scene.getShift());

					scanPositionsFileWriter.println(shifted.getX() + " " + shifted.getY() + " " + shifted.getZ() + " " + percent);
				}

				scanPositionsFileWriter.close();
				// ################### END For each potential scan position, write results of visibility analysis to file ##################

				// ############### BEGIN Select scan position either automatically (best visiblity) or manually ################

				ScanPosition selectedPos = null;

				if (cfg_interactive) {
					System.out.print("Positions analysis file for scan " + scansCount + " written. Continue? ");
					String cont = keyboard.next().toLowerCase();

					if (!(cont.equals("y") || cont.equals("yes") || cont.equals("true"))) {
						break;
					}

					System.out.print("Selected position X: ");
					double x = keyboard.nextDouble();

					System.out.print("Selected position Y: ");
					double y = keyboard.nextDouble();

					System.out.print("Selected position Z: ");
					double z = keyboard.nextDouble();

					selectedPos = new ScanPosition();
					selectedPos.pos = new Vector3D(x, y, z).subtract(scene.getShift());
				} else {

					selectedPos = bestPos;
				}
				// ############### END Select scan position either automatically (best visiblity) or manually ################

				// Add to list of selected positions:
				selectedScanPositions.add(selectedPos);

				Vector3D selectedPosRayOrigin = selectedPos.pos.add(platform.cfg_device_relativeMountPosition);
				HashSet<VisiblePoint> selectedPosVisiblePoints = getVisiblePoints(selectedPosRayOrigin);

				// Remove all points that are visible from the best position from the test points list:
				vtps.removeAll(selectedPosVisiblePoints);

				double totalCoveragePercent = ((float) (numTestPointsTotal - vtps.size()) / (float) numTestPointsTotal) * 100;

				Vector3D shifted = selectedPos.pos.add(scene.getShift());

				System.out.println("Selected: " + shifted + " - " + selectedPos.numVisiblePoints() + " visible points. Remaining points: " + vtps.size()
						+ ". Total coverage so far: " + Math.floor(totalCoveragePercent) + "%");

				if (!cfg_interactive && totalCoveragePercent >= cfg_minCoverage_percent) {
					// LidarSim.log.info("Configured abort coverage of " + cfg_abortCoverage_percent + "% has been achieved. Aborting.");
					break;
				}

				scansCount++;
			}
		}

		keyboard.close();

	}
}
