package de.uni_hd.giscience.helios.core.scanner.detector;

import com.jme3.scene.Node;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.scanner.Scanner;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public class FullWaveformPulseDetector extends AbstractDetector {

	BufferedWriter fullWaveFileWriter = null;
	DecimalFormat df = new DecimalFormat("0.0###");
	
	public FullWaveformPulseDetector(Scanner scanner, double accuracy_m, double range_min) {
		super(scanner, accuracy_m, range_min);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void applySettings(ScannerSettings settings) {
		super.applySettings(settings);
		// Configure pulse simulation:
		//cfg_setting_beamSampleQuality = settings.beamSampleQuality;		
	}

	
	public synchronized void setOutputFilePath(String path) {

		super.setOutputFilePath(path);

		if (path.equals("")) {
			fullWaveFileWriter = null;
			return;
		}

		try {
			File outputFilePath = new File(path);
			outputFilePath.getParentFile().mkdirs();

			fullWaveFileWriter = new BufferedWriter(new FileWriter(outputFilePath + "fullwave.txt"), 2500000);
		} catch (Exception e) {
			fullWaveFileWriter = null;
		}
	}

	@Override
	public void simulatePulse(ExecutorService execService, Vector3D absoluteBeamOrigin, Rotation absoluteBeamAttitude, int state_currentPulseNumber, long currentGpsTime, Node rootNode) {
		// TODO Auto-generated method stub

		// Submit pulse computation task to multithread executor service:
		AbstractPulseRunnable worker = new FullWaveformPulseRunnable(this, absoluteBeamOrigin, absoluteBeamAttitude, state_currentPulseNumber, currentGpsTime, rootNode);

		// Submit pulse runnable to worker threads:
		execService.execute(worker);
	}

	synchronized public void shutdown() {
		super.shutdown();
		
		if (fullWaveFileWriter != null) {
			try {
				fullWaveFileWriter.flush();
				fullWaveFileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// TODO 2: Move this to pulse class (or somewhere else)
	public synchronized void writeFullWave(ArrayList<Double> fullwave, Integer fullwave_index, Double min_time, Double max_time, Vector3D beamOrigin, Vector3D beamDir, Long gpstime) {
		// ############# BEGIN Writefullwave to output file ############
		if (fullWaveFileWriter != null) {
			StringJoiner sj = new StringJoiner(" ");		
			
			sj.add(fullwave_index.toString())
			.add(((Double) beamOrigin.getX()).toString())
			.add(((Double) beamOrigin.getY()).toString())
			.add(((Double) beamOrigin.getZ()).toString())
			.add(((Double) beamDir.getX()).toString())
			.add(((Double) beamDir.getY()).toString())
			.add(((Double) beamDir.getZ()).toString())
			.add(min_time.toString())
			.add(max_time.toString())
			.add(gpstime.toString());	// add GPSTIME (since 1980) in seconds
			for (Double w : fullwave) {
				sj.add(df.format(w));
			}
			sj.add("\n");
			
			String line = sj.toString();

			try {
				fullWaveFileWriter.write(line);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// ############# END Write fullwave to output file ############
	}
}
