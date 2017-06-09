package de.uni_hd.giscience.helios;

import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

import de.uni_hd.giscience.helios.surveyplayback.Survey;
import de.uni_hd.giscience.helios.surveyplayback.XmlSurveyLoader;

/** Commandline Information interpreter for LidarSimulator
 *
 * The commandline interpreter parses and provides information
 * for configuration of the Simulator.
 */
public class CliParser {
  private boolean mIsHeadless = false;
  private final String mDefaultSurveyFile = "data/surveys/demo/tls_arbaro_demo.xml";
  private File mSurveyFile = new File(mDefaultSurveyFile);
  private Survey mSurvey = null;

  private Options mOptions = new Options();
  private final Option mHelpOption = Option.builder("h")
          .longOpt("help")
          .desc("Prints this help")
          .build();
  private final Option mHeadlessOption = Option.builder("nv")
          .longOpt("noVisualization")
          .desc("Executes simulation without visualization")
          .build();
  private final Option mSurveyConfigurationOption = Option.builder("c")
          .longOpt("configFile")
          .desc("Configuration XML file for survey.\n" +
          "Default: " + mDefaultSurveyFile)
          .hasArg()
          .argName("FILE")
          .type(String.class)
          .build();

  public CliParser() {

    mOptions.addOption(mHelpOption);
    mOptions.addOption(mHeadlessOption);
    mOptions.addOption(mSurveyConfigurationOption);
  }

  boolean parse(String[] args) {
    CommandLineParser parser = new DefaultParser();
    boolean hasHelp;
    try {
      CommandLine cmd = parser.parse(mOptions, args);

      hasHelp = cmd.hasOption("h");

      if (!hasHelp) {
        if (cmd.hasOption("nv")) {
          mIsHeadless = true;
        }

        if (cmd.hasOption("c")) {
          mSurveyFile = new File(cmd.getOptionValue("c"));
        }

        checkAndLoadSurveyConfiguration();
      }
    } catch (Exception ex) {
      System.err.println(ex.getLocalizedMessage());
      hasHelp = true;
    }

    if (hasHelp) {
      HelpFormatter formatter = new HelpFormatter();
      String mHeader = "Helios is a LIDAR simulation tool\n\n";
      String mFooter = "\nPlease report issues at https://github.com/GIScience/helios/issues";
      formatter.printHelp ("helios", mHeader, mOptions, mFooter, true);
      return false;
    }

    return true;
  }

  private void checkAndLoadSurveyConfiguration() throws IOException {
    if (!mSurveyFile.exists() || mSurveyFile.isDirectory()) {
      throw new IOException(mSurveyConfigurationOption.getArgName() +
              " does not exit (" + mSurveyFile.getAbsolutePath() + ").");
    } else {
      // Load mSurvey description from XML file:
      XmlSurveyLoader reader = new XmlSurveyLoader(mSurveyFile.getAbsolutePath());
      this.mSurvey = reader.load();

      if (mSurvey == null) {
        throw new IOException(mSurveyConfigurationOption.getArgName() +
                " load failed (" + mSurveyFile.getAbsolutePath() + ").");
      }
    }
  }

  public Survey getSurvey() {
    return this.mSurvey;
  }

  public boolean runHeadless() {
    return this.mIsHeadless;
  }
}
