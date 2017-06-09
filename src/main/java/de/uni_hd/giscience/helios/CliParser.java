package de.uni_hd.giscience.helios;

import de.uni_hd.giscience.helios.surveyplayback.Survey;
import de.uni_hd.giscience.helios.surveyplayback.XmlSurveyLoader;

/** Commandline Information interpreter for LidarSimulator
 *
 * The commandline interpreter parses and provides information
 * for configuration of the Simulator.
 */
public class CliParser {
  private boolean isHeadless = false;
  private String surveyFilePath =  "";
  private Survey mSurvey = null;

  boolean parse(String[] args) {
    // Read mSurvey file from command line argument:
    if (args.length > 0) {
      surveyFilePath = args[0];
    }
    if (args.length > 1) {
      if (args[1].equals("headless")) {
        isHeadless = true;
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

    // Load mSurvey description from XML file:
    XmlSurveyLoader reader = new XmlSurveyLoader(surveyFilePath);
    this.mSurvey = reader.load();

    if (mSurvey == null) {
      System.err.println("Failed to load mSurvey!");
      return false;
    }

    return true;
  }

  public Survey getSurvey() {
    return this.mSurvey;
  }

  public boolean runHeadless() {
    return this.isHeadless;
  }
}
