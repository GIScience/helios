package de.uni_hd.giscience.helios.surveyplayback;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
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

	private int numEffectiveLegs = 0;	// = -1 leg if survey !onGround
	private float elapsedLength = 0;	// Sum of legs length traveled	
	private float progress = 0;
	private float legProgress = 0;			
	private long legStartTime_ns = 0;
	private long elapsedTime_ms = 0;
	private long remainingTime_ms = 0;
	private long legElapsedTime_ms = 0;
	private long legRemainingTime_ms = 0;

	public SurveyPlayback(Survey survey, boolean headless) {
		this.mSurvey = survey;
		this.headless = headless;
		this.exitAtEnd = headless;
		
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
		
		// For progress tracking
		numEffectiveLegs = mSurvey.legs.size();
		if (!mSurvey.legs.get(0).mPlatformSettings.onGround) {
			mSurvey.calculateLength();
			numEffectiveLegs--;
		}
	}
	
	
	void estimateTime(int legCurrentProgress, boolean onGround, double legElapsedLength) {
		
		if (legCurrentProgress > legProgress) {	// Do stuff only if leg progress incremented at least 1%			
			legProgress = legCurrentProgress;
			
			long currentTime = System.currentTimeMillis();
			legElapsedTime_ms = currentTime - legStartTime_ns;
			legRemainingTime_ms = (long) ((100 - legProgress) / (float) legProgress * legElapsedTime_ms);			
			
			if (onGround) {	
				progress = ((mCurrentLegIndex * 100) + legProgress) / (float) numEffectiveLegs;			
			} else {
				progress = (float) ((elapsedLength + legElapsedLength) * 100 / (float) mSurvey.getLength());
			}				
			elapsedTime_ms = currentTime - super.timeStart_ms;;
			remainingTime_ms = (long) ((100 - progress) / (float) progress * elapsedTime_ms);

			if(headless) {							
				DecimalFormat df = new DecimalFormat("0.00");
				System.out.println("Survey " + df.format(progress) + "%\tElapsed " + milliToString(elapsedTime_ms) + " Remaining " + milliToString(remainingTime_ms));
				System.out.println("Leg " + (mCurrentLegIndex + 1) + "/" + numEffectiveLegs + " " + df.format(legProgress) + "%\tElapsed " + milliToString(legElapsedTime_ms) + " Remaining " + milliToString(legRemainingTime_ms));
			}	
		}
	}

	void trackProgress() {
			
		if (getScanner().platform.onGround) {					
			double legElapsedAngle = Math.abs(getScanner().scannerHead.getRotateStart() - getScanner().scannerHead.getRotateCurrent());
			int legProgress = (int) (legElapsedAngle * 100 / getScanner().scannerHead.getRotateRange());
			estimateTime(legProgress, true, 0);		
		
		} else if (mCurrentLegIndex < mSurvey.legs.size() - 1)	{			 		
			double legElapsedLength = Vector3D.distance(getCurrentLeg().mPlatformSettings.getPosition(), mSurvey.scanner.platform.getPosition());
			int legProgress = (int) (legElapsedLength * 100 / getCurrentLeg().getLength());
			estimateTime(legProgress, false, legElapsedLength);
		}	
	}
	
	@Override
	public void doSimStep() {
		
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
			
			legProgress = 0;
			legStartTime_ns = System.currentTimeMillis();
		}
		
		trackProgress();
		
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
		elapsedLength += mSurvey.legs.get(mCurrentLegIndex).getLength();
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
	
	public String milliToString(long millis) {
		long seconds = (millis / 1000) % 60;
		long minutes = (millis / (1000 * 60)) % 60;
		long hours = (millis / (1000 * 60 * 60)) % 24;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}
	
	public float getProgress() {
		return this.progress;
	}
	
	public float getLegProgress() {
		return this.legProgress;
	}
	
	public int getNumEffectiveLegs() {
		return this.numEffectiveLegs;
	}
	
	public long getElapsedTime() {
		return this.elapsedTime_ms;
	}
	
	public long getRemaningTime() {
		return this.remainingTime_ms;
	}
	
	public long getLegElapsedTime() {
		return this.legElapsedTime_ms;
	}
	
	public long getLegRemaningTime() {
		return this.legRemainingTime_ms;
	}
}
