// TODO 2: Merge Simulation and SurveyPlayback classes?

package de.uni_hd.giscience.helios.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import javax.vecmath.Color4f;

import de.uni_hd.giscience.helios.core.scanner.Scanner;
import sebevents.SebEvents;

public abstract class Simulation {

    final Lock lock = new ReentrantLock();
    final Condition condPause  = lock.newCondition();

  private final static long NANOSECONDS_PER_SECOND = 1000000000;
	protected boolean mStopped = false;
	protected boolean mPaused = false;

	private double mSimSpeedFactor = 1;

	private Scanner mScanner = null;

	// TODO 4: Move this to scanner settings?
	public Color4f pointCloudColor = null;

	private ThreadPoolExecutor mExecService = null;

	private long simulationTimeStamp = 0;

	public boolean exitAtEnd = false;
	
	

	
	public MeasurementsBuffer mbuffer = new MeasurementsBuffer();

	public Simulation() {

		// ######### BEGIN Configure pulse simulation threads pool ##########
		int numThreads = Runtime.getRuntime().availableProcessors();
		int queueSize = 5000;
		ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(queueSize, true);
		this.mExecService = new ThreadPoolExecutor(numThreads, numThreads, 1L, TimeUnit.MINUTES, queue, new ThreadPoolExecutor.CallerRunsPolicy());
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
		long timePerStepInNanoSec = Math.round(NANOSECONDS_PER_SECOND / this.mScanner.getPulseFreq_Hz());

		if( mSimSpeedFactor == 0) {
			long now = System.nanoTime();

			while (now - simulationTimeStamp < timePerStepInNanoSec * mSimSpeedFactor) {
				now = System.nanoTime();
			}
			simulationTimeStamp = now;
		} else {
			simulationTimeStamp += timePerStepInNanoSec;
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
      lock.lock();
        condPause.signal();
      lock.unlock();
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

  /**
   * Sets the simulation speed delay factor.

   * @param factor slow down each simulation step by (x*1ns)/PulseFrequencyInHz
   *               If the value is ZERO 0.0 then the simulation runs as fast as possible
   */
	public void setSimSpeedFactor(double factor) {

		if (factor > 10000) {
			factor = 10000;
		}

		this.mSimSpeedFactor = factor;

		System.out.println("Simulation speed set to " + mSimSpeedFactor);
	}

	public void start() {

		long timeStart = System.nanoTime();

		// ############# BEGIN Main simulation loop ############
		while (!isStopped()) {

          lock.lock();
          boolean pause = isPaused();
          lock.unlock();

		  if (pause) {
            lock.lock();
            try {
              condPause.awaitNanos(1000);
            } catch (Exception e) {
              e.printStackTrace();
            } finally {

              lock.unlock();
            }
          } else {
		    doSimStep();
          }
		}
		// ############# END Main simulation loop ############

		long timeMainLoopFinish = System.nanoTime();

		double seconds = (double) (timeMainLoopFinish - timeStart) / 1000000000;

		System.out.println("Main thread simulation loop finished in " + seconds + " sec.");
		System.out.print("Waiting for completion of pulse computation tasks...");

		// ########## BEGIN Loop that waits for the executor service to complete all tasks ###########
		while (true) {

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (mExecService.getQueue().size() == 0) {
				mExecService.shutdown();

				long timeFinishAll = System.nanoTime();

				double secondsAll = (double) (timeFinishAll - timeStart) / 1000000000;

				System.out.print("Pulse computation tasks finished in " + secondsAll + " sec.\n");

				break;
			}
		}
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
