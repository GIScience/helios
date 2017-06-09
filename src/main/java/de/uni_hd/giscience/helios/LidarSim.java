package de.uni_hd.giscience.helios;

import de.uni_hd.giscience.helios.surveyplayback.Survey;
import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;
import de.uni_hd.giscience.helios.visualization.JMEFrontEnd;

/** Initialization point of the application
 *
 * The LidarSim start the program and configure its simulation modes.
 * The application can run as commandline tool for only simulation or
 * in graphical mode for preparation of a simulation or to see
 * the scanning process.
 */
public class LidarSim {

    public static void main(String[] args) {
        CliParser parser = new CliParser();
        if (parser.parse(args)) {
            LidarSim simulation = new LidarSim(parser.getSurvey());

            if (parser.runHeadless()) {
                simulation.executeSimulation();
            } else {
                simulation.startVisualization();
            }
        }
    }

    private SurveyPlayback mPlayback;

    /** Constructor for simulation with a survey which should be simulated.
     *
     * @param survey Survey of interest
     */
    public LidarSim(Survey survey) {
        this.mPlayback = new SurveyPlayback(survey);
    }

    /** Executes the simulation
     *
     * The execution of the simulation immediately and exit on completion.
     * The results are stored for later visualization or
     * post processing by other tools.
     *
     */
    void executeSimulation() {

        mPlayback.setExitOnEndOfSimulation(true);
        mPlayback.setSimSpeedFactor(0);

        mPlayback.start();
    }

    /** Start of visualization GUI
     *
     * This function prepares the backend for running in combination
     * with a graphical front-end. This function will return when
     * the frontend is closed.
     *
     */
    void startVisualization() {

        // Prepare simulation
        // Slow down simulation for visualization
        mPlayback.setExitOnEndOfSimulation(false);
        mPlayback.setSimSpeedFactor(1);

        // Prepare visualization of simulation
        JMEFrontEnd frontend = new JMEFrontEnd();
        frontend.init(mPlayback);
        frontend.start();
        mPlayback.pause(true);

        mPlayback.start();
    }
}
