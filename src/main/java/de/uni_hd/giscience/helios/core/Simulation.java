// TODO 2: Merge Simulation and SurveyPlayback classes?

package de.uni_hd.giscience.helios.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import javax.vecmath.Color4f;

import de.uni_hd.giscience.helios.core.scanner.Scanner;
import sebevents.SebEvents;

public abstract class Simulation {

	private final static long NANOSECONDS_PER_SECOND = 1000000000;
	
	protected boolean mStopped = false;
	protected boolean mPaused = false;

	private double mSimSpeedFactor = 1;

	private Scanner mScanner = null;

	// TODO 4: Move this to scanner settings?
	public Color4f pointCloudColor = null;

	private ThreadPoolExecutor mExecService = null;

	private long mStopwatch = 0;

	public boolean exitAtEnd = false;
	
	public boolean headless = false;
	
	protected long timeStart_ms = 0;
	
	public MeasurementsBuffer mbuffer = new MeasurementsBuffer();

	public Simulation() {

		// ######### BEGIN Configure pulse simulation threads pool ##########
		int numThreads = 1;
		int queueSize = 5000;
		ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(queueSize, true);
		this.mExecService = new ThreadPoolExecutor(numThreads, numThreads, 2L, TimeUnit.MINUTES, queue, new ThreadPoolExecutor.CallerRunsPolicy());
		// ######### END Configure pulse simulation threads pool ##########
	};

	public void doSimStep() {
		
		
		// Check for leg completion:		
		if (mScanner.scannerHead.rotateCompleted() && getScanner().platform.waypointReached()) {
			onLegComplete();
			return;
		}

		mScanner.platform.doSimStep(this.mScanner.getPulseFreq_Hz());
		mScanner.doSimStep(mExecService);

		// ######### BEGIN Real-time brake (slow down simulation to real-world time speed ) #########
		if (!headless) {
			long timePerStep_nanosec = Math.round(NANOSECONDS_PER_SECOND / this.mScanner.getPulseFreq_Hz());
			long now = System.nanoTime();
	
			while (now - mStopwatch < timePerStep_nanosec * mSimSpeedFactor) {
				now = System.nanoTime();
			}
			
			mStopwatch = now;
		}
		// ######### END Real-time brake (slow down simulation to real-world time speed ) #########
	}

	public Scanner getScanner() {
		return this.mScanner;
	}

	public double getSimSpeedFactor() {
		return this.mSimSpeedFactor;
	}

	public boolean isPaused() {
		return this.mPaused;
	}

	public boolean isStopped() {
		return this.mStopped;
	}
	
	protected abstract void onLegComplete();

	public void pause(boolean pause) {
		this.mPaused = pause;
		SebEvents.events.fire("simulation_pause_state_changed", this.mPaused);
	}

	protected void setScanner(Scanner scanner) {
		
		if (scanner == mScanner) {
			return;
		}
		
		System.out.println("Simulation: Scanner changed!");
		
		this.mScanner = scanner;
			
		// Connect measurements buffer:
		if (this.mScanner != null) {
		this.mScanner.detector.mBuffer = this.mbuffer;
		}
		
		SebEvents.events.fire("playback_set_scanner", this.mScanner);
	}

	public void setSimSpeedFactor(double factor) {

		if (factor <= 0) {
			factor = 0.0001;
		}

		if (factor > 10000) {
			factor = 10000;
		}

		this.mSimSpeedFactor = factor;

		System.out.println("Simulation speed set to " + mSimSpeedFactor);
	}

	public void start() {

		double timeStart_ns = System.nanoTime();
		timeStart_ms = System.currentTimeMillis();
		// ############# BEGIN Main simulation loop ############
		while (!isStopped()) {

			if (!isPaused()) {
				doSimStep();
			} else {
				// ATTENTION:
				// For some unknown reason, toggling pause mode only works if the thread
				// sleeps for a short time during each loop iteration (instead of running doSimStep())
				// while the simulation is paused:
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		// ############# END Main simulation loop ############

		long timeMainLoopFinish = System.nanoTime();

		double seconds = (double) (timeMainLoopFinish - timeStart_ns) / 1000000000;

		System.out.println("Main thread simulation loop finished in " + seconds + " sec.");
		System.out.print("Waiting for completion of pulse computation tasks...");

		// ########## BEGIN Loop that waits for the executor service to complete all tasks ###########
		mExecService.shutdown();
		try {
			mExecService.awaitTermination(600, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long timeFinishAll = System.nanoTime();

		double secondsAll = (double) (timeFinishAll - timeStart_ns) / 1000000000;

		System.out.print("Pulse computation tasks finished in " + secondsAll + " sec.\n");
		// ########## END Loop that waits for the executor service to complete all tasks ###########

		// Shutdown the simulation (e.g. close all file output streams. Implemented in derived classes.)
		shutdown();
	}

	public void stop() {
		this.mStopped = true;

	}

	public void shutdown() {

	}

}
