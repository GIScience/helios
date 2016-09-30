package de.uni_hd.giscience.helios.surveyplanning;

import java.util.Comparator;

public class ScanPosComparator implements Comparator<ScanPosition> {

	String mode = "";

	public ScanPosComparator(String mode) {
		this.mode = mode;
	}

	@Override
	public int compare(ScanPosition o1, ScanPosition o2) {

		double num1 = 0;
		double num2 = 0;

		if (mode.equals("meanIncidenceAngle")) {
			num1 = o1.avgIncidenceAngle_rad;
			num2 = o2.avgIncidenceAngle_rad;
		} else if (mode.equals("viewshedSize")) {
			num1 = o1.numVisiblePoints();
			num2 = o2.numVisiblePoints();
		} else if (mode.equals("exclusiveViewshedSize")) {
			num1 = o1.numExclusivePoints();
			num2 = o2.numExclusivePoints();
		} else {
			System.out.println("ERROR: No valid PSP comparison value specified!");
		}

		if (num1 < num2) {
			return -1;
		} else if (num1 > num2) {
			return 1;
		}

		return 0;
	}

}