// TODO 3: Merge xml reader and writer into one class and 
// change element/attribute names to variables that are used by both

package de.uni_hd.giscience.helios.surveyplayback;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;

public class XmlSurveyWriter {

	public static void writeSurveyXmlFile(Survey survey, String filePath) {

		FileWriter fileWriter = null;

		File outputFilePath = new File(filePath);

		try {
			fileWriter = new FileWriter(outputFilePath);
		} catch (IOException e) {
			
			e.printStackTrace();
			return;
		}

		try {
			String sceneString = survey.scanner.platform.scene.getLocationString();

			fileWriter.write("<?xml version=\"1.0\"?>\n");
			fileWriter.write("<document>\n");

			String scannerString = survey.scanner.getLocationString();

			String platformString = survey.scanner.platform.getLocationString();

			fileWriter.write(
					"<survey name=\"" + survey.name + "\" platform=\"" + platformString + "\" scanner=\"" + scannerString + "\" scene=\"" + sceneString + "\">\n");

			// ################# BEGIN Write scan definitions #############

			// TODO 1: Write origin

			for (Leg leg : survey.legs) {
				fileWriter.write("<leg>\n");

				// Write platform settings:
				String pss = "";
				Vector3D pos = new Vector3D(leg.mPlatformSettings.x, leg.mPlatformSettings.y, leg.mPlatformSettings.z);

				Vector3D shifted = pos.add(survey.scanner.platform.scene.getShift());

				// TODO 2: Move platformSettings xml write to separate method
				pss += "<platformSettings ";

				pss += mas("x", String.format("%.3f", shifted.getX()));
				pss += mas("y", String.format("%.3f", shifted.getY()));
				pss += mas("z", String.format("%.3f", shifted.getZ()));
				pss += mas("onGround", leg.mPlatformSettings.onGround);
				pss += "/>";
				fileWriter.write(pss + "\n");

			
				String sss = makeScannerSettingsXml(leg.mScannerSettings);

				fileWriter.write(sss + "\n");

				fileWriter.write("</leg>\n");
			}

			// ################ END Write scan definitions ###############

			fileWriter.write("</survey>\n");
			fileWriter.write("</document>");

			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	static String makeScannerSettingsXml(ScannerSettings settings) {
		String sss = "";
		sss += "<scannerSettings ";

		if (!settings.id.equals("")) {
			sss += mas("id", settings.id);
		}

		sss += mas("active", settings.active);
		sss += mas("pulseFreq_hz", settings.pulseFreq_Hz);
		sss += mas("scanAngle_deg", settings.scanAngle_rad * (180 / Math.PI));
		sss += mas("scanFreq_hz", settings.scanFreq_Hz);
		sss += mas("headRotatePerSec_deg", String.format("%.2f", settings.headRotatePerSec_rad * (180 / Math.PI)));
		sss += mas("headRotateStart_deg" , String.format("%.2f", settings.headRotateStart_rad * (180 / Math.PI)));
		sss += mas("headRotateStop_deg", String.format("%.2f", settings.headRotateStopInRad * (180 / Math.PI)));
		sss += "/>";

		return sss;
	}

	static String mas(String key, Object value) {
		return " " + key + "=\"" + value.toString() + "\"";
	}
}
