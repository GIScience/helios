package de.uni_hd.giscience.helios.core;

import de.uni_hd.giscience.helios.core.scanner.Measurement;

public class MeasurementsBuffer {

	// Measurements buffer:
	final private static int MEASUREMENTS_BUFFER_SIZE = 100000;
	Measurement[] buffer = new Measurement[MEASUREMENTS_BUFFER_SIZE];
	int nextInsertIndex = 1;

	public synchronized void add(Measurement m) {

		// ############# BEGIN Add measurement to measurements buffer ############
		buffer[nextInsertIndex] = m;

		nextInsertIndex++;

		if (nextInsertIndex >= buffer.length - 1) {
			nextInsertIndex = 1;
		}
		// ############# END Add measurement to measurements buffer ############
	}

	public Measurement getEntryAt(int index) {
		return buffer[index];
	}

	public int getLastRecordedPointIndex() {
		return nextInsertIndex - 1;
	}

	
	public int getSize() {
		return buffer.length;
	}
	
}
