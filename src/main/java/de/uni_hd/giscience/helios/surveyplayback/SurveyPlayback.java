package de.uni_hd.giscience.helios.surveyplayback;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.Simulation;
import de.uni_hd.giscience.helios.core.platform.Platform;
import de.uni_hd.giscience.helios.core.platform.PlatformSettings;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;
import sebevents.SebEvents;

public class SurveyPlayback extends Simulation {

	public boolean mLegStarted = false;
	
	public Survey mSurvey = null;

	int mCurrentLegIndex = 0;

	protected int mNumRunsCompleted = 0;

	String mDateString = "";
	String mOutputFilePathString = "";
	String mFormatString = "%03d";

	public SurveyPlayback(Survey survey) {
		this.mSurvey = survey;

		// ######## BEGIN Create part of the leg point cloud file path #######
		Calendar cal = Calendar.getInstance();
		cal.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		mDateString = sdf.format(cal.getTime());
		mOutputFilePathString = "output/" + File.separator + "Survey Playback" + File.separator + mSurvey.name + File.separator + mDateString + File.separator;
		// ######## END Create part of the leg point cloud file path #######

		this.setScanner(mSurvey.scanner);

		// ############### BEGIN If the leg has no survey defined, create a default one ################
		if (mSurvey.legs.size() == 0) {

			Leg leg = new Leg();

			// Set leg scanner settings:
			leg.mScannerSettings = new ScannerSettings();

			// Set leg position to the center of the scene:
			PlatformSettings ps = new PlatformSettings();
			ps.setPosition(mSurvey.scanner.platform.scene.getAABB().getCentroid());

			leg.mPlatformSettings = ps;

			// Add leg to survey:
			mSurvey.addLeg(0, leg);

			SebEvents.events.fire("survey_changed", null);
		}
		// ############### END If the leg has no survey defined, create a default one ################

		// Start the first leg:
		startLeg(0, true);

		// If we start a new scan, move platform to destination of first leg:
		getScanner().platform.setPosition(getCurrentLeg().mPlatformSettings.getPosition());
	}

	@Override
	public void doSimStep() {
		
		/*
		if(mSurvey.legs.size() > 0 ) {
			Vector3D initLegPos = getCurrentLeg().mPlatformSettings.getPosition();
			Vector3D lastLegPos = mSurvey.legs.get(getCurrentLegIndex()+1).mPlatformSettings.getPosition();
			double legsDistance = Vector3D.distance(initLegPos, lastLegPos);
			Vector3D curPlatPos = this.mSurvey.scanner.platform.getPosition();
			double curDistance = Vector3D.distance(curPlatPos, lastLegPos);
			int progress = (int) ((legsDistance - curDistance ) / legsDistance * 100);
			//System.out.println("Progress: " + progress + "%");
		}*/
		
		if (!mLegStarted) {
			// ########## BEGIN Clear point cloud file for current leg ###########
			String outputPath = getCurrentOutputPath();
			FileWriter bla;
			try {
				bla = new FileWriter(outputPath);
				bla.close();
			} catch (IOException e) {

			}
			// ########## END Clear point cloud file for current leg ###########
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

	public String getCurrentOutputPath() {
		return mOutputFilePathString + File.separator + "points" + File.separator + "leg" + String.format(mFormatString, getCurrentLegIndex()) + "_points.xyz";
	}

	@Override
	protected void onLegComplete() {
		startNextLeg(false);
	}

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

		// Specify output file:
		getScanner().detector.setOutputFilePath(getCurrentOutputPath());

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
