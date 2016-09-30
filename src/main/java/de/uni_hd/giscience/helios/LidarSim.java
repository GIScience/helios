package de.uni_hd.giscience.helios;

import java.util.Locale;

import de.uni_hd.giscience.helios.surveyplayback.Survey;
import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;
import de.uni_hd.giscience.helios.surveyplayback.XmlSurveyLoader;
import de.uni_hd.giscience.helios.visualization.JMEFrontEnd;

/*
 * Coordinate system convention for the lidar sim:
 * 
 * x - right
 * y - forward
 * z - up
 */

public class LidarSim {

	// public static Logger log = Logger.getLogger(LidarSim.class.getName());

	public static void main(String[] args) {

		Locale.setDefault(new Locale("en", "US"));

		/*
		 * // ########### BEGIN Set up logger ##############
		 * 
		 * FileHandler fh = null;
		 * 
		 * try { fh = new FileHandler("lidarsim.log"); } catch (SecurityException e) { e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); }
		 * 
		 * log.addHandler(fh); log.setLevel(Level.ALL);
		 * 
		 * SimpleFormatter formatter = new SimpleFormatter(); fh.setFormatter(formatter);
		 * 
		 * // ########### END Set up logger ##############
		 */
		LidarSim app = new LidarSim();
		app.init(args);
	}

	void init(String[] args) {

		String surveyFilePath = "";

		boolean headless = false;

		// Read survey file from command line argument:
		if (args.length > 0) {
			surveyFilePath = args[0];
		}
		if (args.length > 1) {
			if (args[1].equals("headless")) {
				headless = true;
			}

		}

		if (surveyFilePath.equals("")) {
			surveyFilePath = "data/surveys/demo/tls_arbaro_demo.xml";

			surveyFilePath = "data/surveys/demo/mls_tractor.xml";
			surveyFilePath = "data/surveys/demo/felsding.xml";
			

			surveyFilePath = "data/surveys/uav_terrain2.xml";
			
			surveyFilePath = "data/surveys/tractortest.xml";

			surveyFilePath = "data/surveys/als_washington.xml";

			surveyFilePath = "data/surveys/simon/als_terrain2.xml";
			surveyFilePath = "data/surveys/simon/tls_simon_trail.xml";
			
			
			
			
			surveyFilePath = "data/surveys/demo/tls_terrain1.xml";
			
			surveyFilePath = "data/surveys/premium/tls_hrusov_castle.xml";
			surveyFilePath = "data/surveys/demo/tls_arbaro_demo.xml";
		}

		// Load survey description from XML file:
		XmlSurveyLoader xmlreader = new XmlSurveyLoader(surveyFilePath);
		Survey survey = xmlreader.load();

		if (survey == null) {
			System.out.println("Failed to load survey!");
			System.exit(-1);
		}

		SurveyPlayback playback = new SurveyPlayback(survey);
		playback.exitAtEnd = headless;

		// ############ BEGIN Start visualization module #############

		if (!headless) {
			JMEFrontEnd frontend = new JMEFrontEnd();
			frontend.init(playback);
			frontend.start();
			playback.pause(true);
		}
		// ############ END Start visualization module #############

		System.out.print("Running simulation...");

		playback.start();
	}
}
