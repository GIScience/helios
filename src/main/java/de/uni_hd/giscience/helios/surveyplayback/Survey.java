package de.uni_hd.giscience.helios.surveyplayback;

import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.Asset;
import de.uni_hd.giscience.helios.core.scanner.Scanner;
import sebevents.SebEvents;

public class Survey extends Asset {

	public String name = "Unnamed Survey Playback";
	public int numRuns = -1;

	public Scanner scanner;

	public double simSpeedFactor = 1;

	public ArrayList<Leg> legs = new ArrayList<>();
	private double length = 0;	// Distance passing through all the legs


	public void addLeg(int insertIndex, Leg leg) {

		if (!legs.contains(leg)) {
			legs.add(insertIndex, leg);
			SebEvents.events.fire("survey_changed", null);
			SebEvents.events.fire("leg_added", leg);
		}
	}

	public void removeLeg(int legIndex) {
		legs.remove(legIndex);

		// TODO 3: Do we handle both events?
		SebEvents.events.fire("survey_changed", null);
		SebEvents.events.fire("leg_removed", null);
	}
	
	public void calculateLength() {	
		for (int i = 0; i < legs.size() - 1; i++) {
			legs.get(i).setLength(Vector3D.distance(legs.get(i).mPlatformSettings.getPosition(), legs.get(i+1).mPlatformSettings.getPosition()));
			length += legs.get(i).getLength();
		}
	}
	
	public double getLength() {
		return this.length;
	}
}
