package de.uni_hd.giscience.helios.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import de.uni_hd.giscience.helios.core.scanner.Scanner;
import sebevents.SebEvents;

/**
 * The simulation class coordinates the simulation and triggers simulation steps until the simulation is stopped.
 * Each simulation step will simulate the motion of the scanner and then the simulation of the beams
 * and point detection of the scanner.
 */
public abstract class Simulation {

	/**
	 * Convert term ns/s
	 */
	private final static long NANOSECONDS_PER_SECOND = 1000000000;

	/**
	 * Defines the count of maximale queue runnabled before rejection of the task
	 */
	private final static int THREAD_QUEUE_LENGTH = 5000;

	/**
	 * Thread pool for for all ray pulse simulations
	 */
	private ThreadPoolExecutor mExecService = null;

	/**
	 * Marker for simulation to stop next simulation step
	 */
	protected boolean mStopped = false;

	/**
	 * Marker to break the simulation until the user want to resume
	 */
	protected boolean mPaused = false;

	/**
	 * Simulation speed factor is used to slow down the simulation for better visualisation or
	 * for performance detect performance issues
	 */
	private double mSimSpeedFactor = 1;

	/**
	 * Contains the scanner which should be used in the simulation.
	 * This scanner type will transfer the scene into a simulated point cloud.
	 */
	private Scanner mScanner = null;

    /**
     * Contains the timestamp (nanoSec) of the last simulation step.
     */
	private long mStopwatch = 0;

    /**
     * Storage for the calculated points for visualisation
     */
	public MeasurementsBuffer mPointBuffer = new MeasurementsBuffer();

    /**
     * Constructor simulation
     */
	public Simulation() {
		// Configure pulse simulation threads pool
		int numThreads = Runtime.getRuntime().availableProcessors();
		ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(THREAD_QUEUE_LENGTH, true);
		this.mExecService = new ThreadPoolExecutor(
				numThreads, numThreads,
				1L, TimeUnit.MINUTES,
				queue, new ThreadPoolExecutor.CallerRunsPolicy()
		);
	};

    public Scanner getScanner() {
		return this.mScanner;
	}

    protected void setScanner(Scanner scanner) {

        if (scanner == mScanner) {
            return;
        }

        System.out.println("Simulation: Scanner changed!");

        this.mScanner = scanner;

        // Connect measurements buffer:
        if (this.mScanner != null) {
            this.mScanner.detector.mBuffer = this.mPointBuffer;
        }

        SebEvents.events.fire("playback_set_scanner", this.mScanner);
    }

	public double getSimSpeedFactor() {
		return this.mSimSpeedFactor;
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

	public boolean isPaused() {
		return this.mPaused;
	}

    public void pause(boolean pause) {
        this.mPaused = pause;
        SebEvents.events.fire("simulation_pause_state_changed", this.mPaused);
    }

	public boolean isStopped() { return this.mStopped; }

    public void stop() { this.mStopped = true; }

	public void start() {

		long timeStart = System.nanoTime();
        long simulationTimeStep = Math.round(NANOSECONDS_PER_SECOND / this.mScanner.getPulseFreq_Hz());

		// ############# BEGIN Main simulation loop ############
		while (!isStopped()) {

			if (!isPaused()) {
                // Check for leg completion:
                if (mScanner.scannerHead.rotateCompleted() && getScanner().platform.waypointReached()) {
                    onLegComplete();
                } else {
                    // Run simulation
                    doSimStep();
                    DelaySimulation( mSimSpeedFactor, simulationTimeStep);
                }
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
		double seconds = (double) (timeMainLoopFinish - timeStart) / NANOSECONDS_PER_SECOND;

		System.out.println("Main thread simulation loop finished in " + seconds + " sec.");
		System.out.print("Waiting for completion of pulse computation tasks...");

		WaitForExecutorService(timeStart);

		long timeFinishAll = System.nanoTime();
		double secondsAll = (double) (timeFinishAll - timeStart) / NANOSECONDS_PER_SECOND;
		System.out.print("Pulse computation tasks finished in " + secondsAll + " sec.\n");

		// Shutdown the simulation (e.g. close all file output streams. Implemented in derived classes.)
		shutdown();
	}

    protected void doSimStep() {
        mScanner.platform.doSimStep(this.mScanner.getPulseFreq_Hz());
        mScanner.doSimStep(mExecService);
    }

    private void DelaySimulation(double speedFactor, long timeStepsInNanoSec) {
        final double delayEndTime = timeStepsInNanoSec * speedFactor;

        long now = System.nanoTime();
        while ((now - mStopwatch) < delayEndTime) {
            now = System.nanoTime();
        }

        mStopwatch = now;
    }

    private void WaitForExecutorService(long timeStart) {
		while (mExecService.getQueue().size() != 0) {

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mExecService.shutdown();
	}

    /**
     * This function is triggered on stop of simulation.
     * It can be overridden to finish storing of simulation results.
     */
	public void shutdown() {}

    /**
     * This function is triggered if the the scanner rotation is done and a way point is reached.
     */
    protected abstract void onLegComplete();

}
