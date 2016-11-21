package de.uni_hd.giscience.helios.core;

import de.uni_hd.giscience.helios.core.scanner.Measurement;

/**
 * Buffer for measurement values which clears the buffer on overflow.
 * This is a basic version of a ring buffer. It has no collision detection on read/write overflow.
 */
public class MeasurementsBuffer {

	// Measurements buffer:
	final private static int MEASUREMENTS_BUFFER_SIZE = 100000;
	Measurement[] buffer = new Measurement[MEASUREMENTS_BUFFER_SIZE];
	int lastWrittenIndex = 0;

	/**
	 * Adds a measurement, on overflow the measurement is added to the first entry of the buffer
	 * @param m measurement to add
	 */
	public synchronized void add(Measurement m) {

        lastWrittenIndex++;

        if (lastWrittenIndex >= MEASUREMENTS_BUFFER_SIZE) {
            lastWrittenIndex = 0;
        }

		buffer[lastWrittenIndex] = m;
	}

    /**
     * Return measurment from index
     * @param index Position of measurement
     * @return measurement on index
     */
	public Measurement getEntryAt(int index) {
		return buffer[index];
	}

	/**
	 * Returns the index of the last appended measurement
	 * @return index of last entry
	 *
	 */
	public int getLastRecordedPointIndex() {
		return lastWrittenIndex;

	}

	/**
	 * Returns the maximum count of measurements
	 * @return capacity of buffer
	 */
	public int getSize() {
		return MEASUREMENTS_BUFFER_SIZE;
	}
	
}
