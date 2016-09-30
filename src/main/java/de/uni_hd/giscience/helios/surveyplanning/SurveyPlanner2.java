package de.uni_hd.giscience.helios.surveyplanning;

import java.util.HashSet;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class SurveyPlanner2 extends AbstractSurveyPlanner {

	@Override
	public void run() {

		// ################ BEGIN Find out if scan position is redundant or not ################

		long timeStart = System.nanoTime();

		ScanPosComparator redundantPSPsComparator = new ScanPosComparator(this.cfg_pspRemoveOrder);
		ScanPosComparator dispensbalePSPsComparator = new ScanPosComparator("exclusiveViewshedSize");

		System.out.println("Removing redundant PSPs (order: " + this.cfg_pspRemoveOrder + ") ...");

		// ######### BEGIN For each PSP, determine exclusivity and remove redundant PSPs ############

		while (true) {

			// ################ BEGIN For each position, find exclusive points and remove redundant scan positions ###################

			// Order PSPs by viewshed size (smallest first):
			java.util.Collections.sort(psps, redundantPSPsComparator);

			while (true) {

				// ######### BEGIN For each PSP, determine exclusivity and remove redundant PSPs ############

				ScanPosition removePos = null;

				// ######## BEGIN Compute exclusivity for all PSPs ########
				for (ScanPosition stp : psps) {

					for (Vector3D v : stp.visiblePoints) {

						boolean pointIsExclusive = true;

						for (ScanPosition other : psps) {

							if (other != stp && other.visiblePoints.contains(v)) {
								pointIsExclusive = false;
								break;
							}
						}

						if (pointIsExclusive && !stp.exclusivePoints.contains(v)) {
							stp.exclusivePoints.add(v);
						}
					}

					// If PSP is redundant, select it for removal and break the for loop:
					if (stp.isRedundant()) {
						removePos = stp;
						break;
					}
				}
				// ######## END Compute exclusivity for all PSPs ########

				// If there still is a redundant pos, remove it. Otherwise, exit the while loop:
				if (removePos != null) {
					psps.remove(removePos);
				} else {
					break;
				}
			}
			// ######### END For each PSP, determine exclusivity and remove redundant PSPs ############ }

			// Compuate coverage percentage:
			HashSet<Vector3D> coveredPoints = new HashSet<>();

			for (ScanPosition stp : psps) {
				coveredPoints.addAll(stp.visiblePoints);
			}

			// Sort by number of exclusive points in order to remove the PSP with the fewest exclusive points:
			java.util.Collections.sort(psps, dispensbalePSPsComparator);

			if (psps.size() > 0) {
				int nextRemovePosViewshedSize = psps.get(0).numExclusivePoints();

				double coverage_percent = (((double) coveredPoints.size() - nextRemovePosViewshedSize) / this.vtps.size()) * 100;

				// Remove PSPs until coverage drops below threshold percentage:
				if (coverage_percent > cfg_minCoverage_percent) {
					psps.remove(0);
				} else {
					break;
				}
			} else {
				break;
			}
		}

		long timeFinish = System.nanoTime();
		double seconds = (double) (timeFinish - timeStart) / 1000000000;

		System.out.println("finished in " + seconds + " seconds. # Test points: " + vtps.size());
		System.out.println("# Required positions: " + psps.size() + " of " + numPotentialScanPositionsTotal);

		// ######### END For each PSP, determine exclusivity and remove redundant PSPs ############

		// Add remaining positions to list of selected scan positions:
		selectedScanPositions.addAll(psps);

		// ########## BEGIN Count positions where # of exclusive points is greater than threshold ###########

		// System.out.println(selectedScanPositions.size() + " positions with more than " + minCoveragePercent + " exclusive points found.");
		// ########## END Count positions where # of exclusive points is greater than threshold ###########

		printSelectedPositionsInfo();
	}
}
