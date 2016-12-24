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
  /**
   * http://stackoverflow.com/questions/20521750/ticks-between-unix-epoch-and-gps-epoch
   * SimpleDateFormat df = new SimpleDateFormat();
   * df.setTimeZone(TimeZone.getTimeZone("UTC"));
   *
   * Date x = df.parse("1.1.1970 00:00:00");
   * Date y = df.parse("6.1.1980 00:00:00");
   *
   * long diff = y.getTime() - x.getTime();
   * long diffSec = diff / 1000;
   */
  private final static long UNIX_TIME_TO_GPS_TIME = 315964800; // sec
  private final static long NANOSECONDS_PER_SECOND = 1000000000;

	private boolean mStopped = false;
    private final Lock lockStop = new ReentrantLock();

	private boolean mPaused = false;
    private final Lock lockPause = new ReentrantLock();
    private final Condition condPause  = lockPause.newCondition();

	private double simSpeedFactor = 1;

	private Scanner mScanner = null;

	// TODO 4: Move this to scanner settings?
	public Color4f pointCloudColor = null;

	private ThreadPoolExecutor mExecService = null;

	private long simulationDiffTimeInNs = 0;
    private long simulationTimeStampUnixInMs = System.currentTimeMillis();

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

		Long gpsTimeInSec = (simulationTimeStampUnixInMs + (simulationDiffTimeInNs / 1000)) / 1000 + UNIX_TIME_TO_GPS_TIME;

		mScanner.platform.doSimStep(this.mScanner.getPulseFreq_Hz());
      	mScanner.doSimStep(mExecService, gpsTimeInSec);

		// ######### BEGIN Real-time brake (slow down simulation to real-world time speed ) #########
		long timePerStepInNanoSec = Math.round(NANOSECONDS_PER_SECOND / this.mScanner.getPulseFreq_Hz());
        final double simFactor = getSimSpeedFactor();

		if( simFactor == 0) {
			long now = System.nanoTime();

			while (now - simulationDiffTimeInNs < timePerStepInNanoSec * simFactor) {
				now = System.nanoTime();
			}
			simulationDiffTimeInNs = now;
		} else {
			simulationDiffTimeInNs += timePerStepInNanoSec;
		}
		// ######### END Real-time brake (slow down simulation to real-world time speed ) #########
	}

	public Scanner getScanner() {
		return this.mScanner;
	}

	public double getSimSpeedFactor() {
		return this.simSpeedFactor;
	}

    /**
     * Sets the simulation speed delay factor.

     * @param factor slow down each simulation step by (factor*1ns)/PulseFrequencyInHz
     *               If the value is ZERO 0.0 then the simulation runs as fast as possible
     */
    public void setSimSpeedFactor(double factor) {

      if (factor > 10000) {
        factor = 10000;
      }

      this.simSpeedFactor = factor;
    }

    public boolean isStopped() {
      lockStop.lock();
      boolean stop = this.mStopped;
      lockStop.unlock();

      return stop;
    }

    public void stop() {
      lockStop.lock();
      this.mStopped = true;
      lockStop.unlock();
    }

    public boolean isPaused() {
      lockPause.lock();
      boolean pause = this.mPaused;
      lockPause.unlock();

      return pause;
	}

	public void pause(boolean pause) {
      lockPause.lock();
      this.mPaused = pause;
      condPause.signal();
      lockPause.unlock();

      SebEvents.events.fire("simulation_pause_state_changed", this.mPaused);
	}

    protected abstract void onLegComplete();

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

	public void start() {

		long timeStart = System.nanoTime();

		// ############# BEGIN Main simulation loop ############
		while (!isStopped()) {

          boolean pause = isPaused();

		  if (pause) {
            lockPause.lock();
            try {
              condPause.awaitNanos(1000);
            } catch (Exception e) {
              e.printStackTrace();
            } finally {

              lockPause.unlock();
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
		waitForPulseComputationCompleted( (countWaitingPulses) -> {
            String output = String.format( "\r %d running pulse computations, waiting for complete.",
                  countWaitingPulses);

            System.out.print( output);
            }
        );
        mExecService.shutdown();


        long timeFinishAll = System.nanoTime();
        double secondsAll = (double) (timeFinishAll - timeStart) / 1000000000;
        System.out.print("\rPulse computation tasks finished in " + secondsAll + " sec.\n");

		// Shutdown the simulation (e.g. close all file output streams. Implemented in derived classes.)
		shutdown();
	}

  /**
   * Defines a callback interface for the pulse calculation worker task
   */
  protected interface ICountWaitingTasksChanged {
    /**
     * Callback is called when the count of running executors has changed.
     * @param countWaitingTasks count of still open tasks
     */
    void update(long countWaitingTasks);
  }

  /**
   * Waits until completion of all pulse computations.
   * @param callback Callback informs user about count waiting tasks changed
   */
  protected void waitForPulseComputationCompleted( ICountWaitingTasksChanged callback) {
      long countWaitingTasks = mExecService.getQueue().size();
      long lastCountWaitingTasks;

      while ( countWaitingTasks != 0) {

        // Delay completion check
        try {
          Thread.sleep(100); // ms
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        lastCountWaitingTasks = countWaitingTasks;
        countWaitingTasks = mExecService.getQueue().size();

        // Check for counter change
        if( countWaitingTasks != lastCountWaitingTasks) {
          if( callback != null) {
            callback.update(countWaitingTasks);
          }
        }
      }
    }


	public void shutdown() {

	}

}
