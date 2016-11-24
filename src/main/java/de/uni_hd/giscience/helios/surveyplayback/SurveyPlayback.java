package de.uni_hd.giscience.helios.surveyplayback;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.uni_hd.giscience.helios.core.Simulation;
import de.uni_hd.giscience.helios.core.platform.Platform;
import de.uni_hd.giscience.helios.core.platform.PlatformSettings;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;
import sebevents.SebEvents;

public class SurveyPlayback extends Simulation {

	public boolean mLegStarted = false;
	
	public Survey mSurvey = null;

	int mCurrentLegIndex = 0;

	String mDateString = "";
	String mOutputFilePathString = "";
	String mFormatString = "%03d";

	public boolean exitAtEnd = false;

	public SurveyPlayback(Survey survey) {
		this.mSurvey = survey;
		this.mOutputFilePathString = CreateBasePath();

		this.setScanner(mSurvey.scanner);

		// create default leg/stage when no leg is created
		if (mSurvey.legs.size() == 0) {

			// Set leg position to the center of the scene:
			Leg leg = new Leg();
			leg.mPlatformSettings.setPosition(mSurvey.scanner.platform.scene.getAABB().getCentroid());

			mSurvey.addLeg(0, leg);

			SebEvents.events.fire("survey_changed", null);
		}

		// Prepare simulation with first leg
		startLeg(0, true);

		// If we start a new scan, move platform to destination of first leg:
		getScanner().platform.setPosition(getCurrentLeg().mPlatformSettings.getPosition());
	}

	/**
	 * Creates a base path to store simulation results
	 * @return relative path for output simulation results
	 */
	protected String CreateBasePath() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		mDateString = sdf.format(cal.getTime());
		return "output/" + File.separator + "Survey Playback" + File.separator + mSurvey.name + File.separator + mDateString + File.separator;
	}

	/**
	 * Creates the result filename with path for the present leg
	 * @return relative file path
	 */
	public String getCurrentOutputPath() {
		return mOutputFilePathString + File.separator + "points" + File.separator + "leg" + String.format(mFormatString, getCurrentLegIndex()) + "_points.xyz";
	}

	/**
	 * Prepare the result output file on each simulation start
	 */
	@Override
	public void doSimStep() {

		if (!mLegStarted) {
			File f = new File(getCurrentOutputPath());
			if( f.exists())
			{
				f.delete();
				System.out.println("WARNING: Old file detected, this file was deleted (" + f.getAbsolutePath() + ")");
			}
			getScanner().detector.setOutputFilePath(getCurrentOutputPath());

			mLegStarted = true;
		}

		super.doSimStep();
	}

	public Leg getCurrentLeg() {

		if (mCurrentLegIndex < mSurvey.legs.size()) {
			return mSurvey.legs.get(mCurrentLegIndex);
		}

		// NOTE: This should never happen:
		System.out.println("ERROR getting current leg: Index out of bounds");
		return null;
	}

	public int getCurrentLegIndex() {
		return mCurrentLegIndex;
	}

	public void removeCurrentLeg() {

		// Always keep at least one leg:
		if (mSurvey.legs.size() < 2) {
			return;
		}

		int removeIndex = getCurrentLegIndex();
		mSurvey.removeLeg(removeIndex);

		// Switch to another leg:
		if (removeIndex > 0) {
			startLeg(removeIndex - 1, true);
		} else {
			startLeg(removeIndex, true);
		}
	}



	@Override
	protected void onLegComplete() {
		startNextLeg(false);
	}

	/**
	 * ????? is this used in start of simulation or is this used when the user switch between stage/leg points???
	 * @param legIndex
	 * @param manual
	 */
	public void startLeg(int legIndex, boolean manual) {

		if (legIndex < 0 || legIndex >= mSurvey.legs.size()) {
			return;
		}

		System.out.println("Starting leg " + legIndex);

		mLegStarted = false;

		mCurrentLegIndex = legIndex;

		Leg leg = getCurrentLeg();

		// Apply scanner settings:
		if (leg.mScannerSettings != null) {
			mSurvey.scanner.applySettings(leg.mScannerSettings);
		}

		Platform platform = getScanner().platform;

		// Apply platform settings:
		if (leg.mPlatformSettings != null) {
			platform.applySettings(leg.mPlatformSettings, manual);

			// ################ BEGIN Set platform destination ##################
			int nextLegIndex = legIndex + 1;

			if (nextLegIndex < mSurvey.legs.size()) {
				// Set destination to position of next leg:
				Leg nextLeg = mSurvey.legs.get(nextLegIndex);

				// TODO 4: Don't do this for platforms that don't move
				platform.setDestination(nextLeg.mPlatformSettings.getPosition());
			}

			if (manual) {
				platform.initLegManual();
			}
			// ################ END Set platform destination ##################
		}

		SebEvents.events.fire("playback_init_leg", getCurrentLeg());
	}

	public void startNextLeg(boolean manual) {

		if (mCurrentLegIndex < mSurvey.legs.size() - 1) {
			// If there are still legs left, start the next one:
			startLeg(mCurrentLegIndex + 1, manual);
		} else {
			// If this was the final leg, stop the simulation:
			if (exitAtEnd) {
				shutdown();
				stop();
			} else {
				pause(true);
			}
		}
	}

	@Override
	public void shutdown() {
		super.shutdown();

		mSurvey.scanner.detector.shutdown();
	}

	public void writeToFile(String filePath) {
		XmlSurveyWriter.writeSurveyXmlFile(this.mSurvey, filePath);

		System.out.println("saved!");
	}
}
