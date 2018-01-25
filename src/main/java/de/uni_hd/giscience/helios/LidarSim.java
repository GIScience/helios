package de.uni_hd.giscience.helios;

import java.util.Locale;

import de.uni_hd.giscience.helios.surveyplayback.Survey;
import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;
import de.uni_hd.giscience.helios.surveyplayback.XmlSurveyLoader;
import de.uni_hd.giscience.helios.visualization.JMEFrontEnd;

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
			System.out.println("Error: Input survey not specified");
			System.exit(-1);
		}

		// Load survey description from XML file:
		XmlSurveyLoader xmlreader = new XmlSurveyLoader(surveyFilePath);
		Survey survey = xmlreader.load();

		if (survey == null) {
			System.out.println("Failed to load survey!");
			System.exit(-1);
		}

		SurveyPlayback playback = new SurveyPlayback(survey, headless);

		// ############ BEGIN Start visualization module #############

		if (!headless) {
			JMEFrontEnd frontend = new JMEFrontEnd();
			frontend.init(playback);

			frontend.start();
			playback.pause(true);
		}
		// ############ END Start visualization module #############

		System.out.println("Running simulation...");

		playback.start();
	}
}
