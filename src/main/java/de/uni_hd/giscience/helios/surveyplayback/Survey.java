package de.uni_hd.giscience.helios.surveyplayback;

import java.util.ArrayList;

import de.uni_hd.giscience.helios.core.Asset;
import de.uni_hd.giscience.helios.core.scanner.Scanner;
import sebevents.SebEvents;

public class Survey extends Asset {

	public String name = "Unnamed Survey Playback";
	public int numRuns = -1;

	public Scanner scanner;

	public double simSpeedFactor = 1;

	public ArrayList<Leg> legs = new ArrayList<>();


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

}
