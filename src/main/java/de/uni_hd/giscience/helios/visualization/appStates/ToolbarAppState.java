package de.uni_hd.giscience.helios.visualization.appStates;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.ProgressBar;

import de.uni_hd.giscience.helios.core.platform.PlatformSettings;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;
import de.uni_hd.giscience.helios.surveyplayback.Leg;
import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;
import sebevents.EventListener;
import sebevents.SebEvents;

public class ToolbarAppState extends BaseAppState implements EventListener {

	boolean mAutoGroundDefault = false;

	Leg mCurrentLegCached = null;

	Label mLabel_platformPos;

	int cameraMode = 0;

	// ########### BEGIN Lemur UI elements ###############
	Checkbox mCheckbox_autoground = null;
	Checkbox mCheckbox_scannerActive = null;

	TextField mTextField_scanAngle_deg = null;
	TextField mTextField_headRotatePerSec_deg = null;
	TextField mTextField_pulseFreq_hz = null;
	TextField mTextField_scanFreq_hz = null;

	Button mButton_sim_pause = null;
	
	ProgressBar mProgressBar_survey = null;
	ProgressBar mProgressBar_leg = null;
	Label mLabel_surveyElapsed = null;
	Label mLabel_surveyRemaining = null;
	Label mLabel_legElapsed = null;
	Label mLabel_legRemaining = null;
	
	// ########### ENDLemur UI elements ###############

	boolean mEditWaypointsEnabled = false;


	public ToolbarAppState(SurveyPlayback sim) {
		super(sim);

		SebEvents.events.addListener("simulation_pause_state_changed", this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		int windowHeight = mApp.mSettings.getHeight();
		int button_margin = 16;
		int headingSize = 20;

		ColorRGBA white = new ColorRGBA(1f, 1f, 1f, 1f);

		
		// ########### BEGIN Set up save survey dialog ##########
		Container saveSurveyDialog = new Container();

		saveSurveyDialog.setLocalTranslation(200, windowHeight - 50, 0);

		Label label_bla2 = new Label("Save survey to file:");

		label_bla2.setColor(white);
		label_bla2.setFontSize(headingSize);
		saveSurveyDialog.addChild(label_bla2);

		TextField mTextField_save = new TextField("");
		mTextField_save.setPreferredWidth(300f);
		mTextField_save.setText(sim.mSurvey.sourceFilePath);
		saveSurveyDialog.addChild(mTextField_save);

		Button button_okay = saveSurveyDialog.addChild(new Button("Save"));
		button_okay.setTextHAlignment(HAlignment.Center);
		button_okay.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {

				try {
					mPlayback.writeToFile(mTextField_save.getText());
				} catch (Exception e) {

				}

				saveSurveyDialog.removeFromParent();
			}
		});

		Button button_cancel = saveSurveyDialog.addChild(new Button("Cancel"));
		button_cancel.setTextHAlignment(HAlignment.Center);
		button_cancel.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				saveSurveyDialog.removeFromParent();
			}
		});

		// ########### END Set up save survey dialog ##########

		// ############# BEGIN Set up Main Panel ################

		Container myWindow = new Container();

		mApp.getGuiNode().attachChild(myWindow);

		// Put it somewhere that we will see it.
		// Note: Lemur GUI elements grow down from the upper left corner.
		myWindow.setLocalTranslation(0, windowHeight, 0);

		mLabel_platformPos = new Label("");
		mLabel_platformPos.setLocalTranslation(170, windowHeight - 8, 0);
		mLabel_platformPos.setColor(new ColorRGBA(0, 0, 0, 1));

		mApp.getGuiNode().attachChild(mLabel_platformPos);

		Label label_bla = new Label("GENERAL");

		label_bla.setColor(white);
		label_bla.setFontSize(headingSize);
		myWindow.addChild(label_bla);

		Button button_save = new Button("Save Survey XML");
		button_save.setTextHAlignment(HAlignment.Center);

		myWindow.addChild(button_save);

		button_save.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				mApp.getGuiNode().attachChild(saveSurveyDialog);
			}
		});

		Button button_camera = myWindow.addChild(new Button("Enable Free Camera"));
		button_camera.setTextHAlignment(HAlignment.Center);
		button_camera.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				cameraMode = ++cameraMode % 2;

				String label = "Enable Free Camera";
				switch (cameraMode) {
				case 1:
					label = "Enable Chase Camera";
					break;
				}

				source.setText(label);

				mApp.setCameraMode();
			}
		});

		Button button_clear_buffer = new Button("Clear Points Buffer");
		button_clear_buffer.setTextHAlignment(HAlignment.Center);
		myWindow.addChild(button_clear_buffer);

		button_clear_buffer.setLocalTranslation(120, windowHeight - button_margin, 0);
		button_clear_buffer.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {

				SebEvents.events.fire("cmd_clear_points_buffer", null);
			}
		});

		Button button_exit = new Button("Exit");
		button_exit.setTextHAlignment(HAlignment.Center);
		myWindow.addChild(button_exit);

		// mApp.getGuiNode().attachChild(button_exit);
		button_exit.setLocalTranslation(120, windowHeight - button_margin, 0);
		button_exit.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				System.out.println("Bye!");
				sim.stop();
				mApp.stop();

			}
		});
		// ############# END Set up Main Panel ################

		// ####################### BEGIN Set up sim control panel ######################
		Container simControlPanel = new Container();
		// mApp.getGuiNode().attachChild(simControlPanel);
		simControlPanel.setLocalTranslation(500, 1024, 0);

		simControlPanel = myWindow;

		Label label_simControl = new Label("SIM CONTROL");

		label_simControl.setColor(white);
		label_simControl.setFontSize(headingSize);
		simControlPanel.addChild(label_simControl);

		mButton_sim_pause = simControlPanel.addChild(new Button(sim.isPaused() ? "Play" : "Pause"));
		mButton_sim_pause.setTextHAlignment(HAlignment.Center);
		mButton_sim_pause.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {

				mApp.setActiveTool(null);
				sim.pause(!sim.isPaused());

			}
		});

		Button button_sim_slower = simControlPanel.addChild(new Button("Slower"));
		button_sim_slower.setTextHAlignment(HAlignment.Center);
		button_sim_slower.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				sim.setSimSpeedFactor(sim.getSimSpeedFactor() * 2);
			}
		});

		Button button_sim_faster = simControlPanel.addChild(new Button("Faster"));
		button_sim_faster.setTextHAlignment(HAlignment.Center);
		button_sim_faster.addClickCommands(new Command<Button>() {
			@Override
			public void execute(final Button source) {
				sim.setSimSpeedFactor(sim.getSimSpeedFactor() / 2);

				// simSpeedLabel.setText("Speed: " + 1.0 / sim.getSimSpeedFactor() + " x");
			}
		});

		/*
		Button button_sim_realtime = simControlPanel.addChild(new Button("Real-Time"));
		button_sim_realtime.setTextHAlignment(HAlignment.Center);
		button_sim_realtime.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				sim.setSimSpeedFactor(1);

				// simSpeedLabel.setText("Speed: " + 1.0 / sim.getSimSpeedFactor() + " x");
			}
		});
*/
		// ######################## END Set up sim control panel ####################

		// ######################## BEGIN Set up process panel ####################
		Label label_progress = new Label("PROGRESS");
		label_progress.setColor(white);
		label_progress.setFontSize(headingSize);
		mLabel_surveyElapsed = new Label("Elapsed");
		mLabel_surveyRemaining = new Label("Remaining");
		mLabel_legElapsed = new Label("Elapsed");
		mLabel_legRemaining = new Label("Remaining");
		
		mProgressBar_survey = new ProgressBar();
		mProgressBar_survey.setMessage("Survey");
		mProgressBar_leg = new ProgressBar();
		mProgressBar_leg.setMessage("Leg 0/" + sim.getNumEffectiveLegs());
		
		myWindow.addChild(label_progress);
		myWindow.addChild(mProgressBar_survey);
		myWindow.addChild(mLabel_surveyElapsed);
		myWindow.addChild(mLabel_surveyRemaining);
	    myWindow.addChild(mProgressBar_leg);
	    myWindow.addChild(mLabel_legElapsed);
		myWindow.addChild(mLabel_legRemaining);
		// ######################## END Set up process panel ####################
		
		// ###################### BEGIN Set Up Waypoint Edit Panel #####################

		Container waypointEditPanel = myWindow;

		Label label_waypoints = new Label("WAYPOINT");
		label_waypoints.setColor(white);
		label_waypoints.setFontSize(headingSize);

		waypointEditPanel.addChild(label_waypoints);

		Button button_prev_waypoint = waypointEditPanel.addChild(new Button("<< Back"));
		button_prev_waypoint.setTextHAlignment(HAlignment.Center);
		button_prev_waypoint.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				sim.pause(true);

				// If the current leg was already started, clicking the "Back" button should
				// first reset the current leg, but not yet switch to the previous leg:
				if (sim.mLegStarted) {
					sim.startLeg(sim.getCurrentLegIndex(), true);
				}

				else {
					// If the current leg was not started yet, we go back to the beginning of the previous leg:
					sim.startLeg(sim.getCurrentLegIndex() - 1, true);
				}
			}
		});

		Button button_next_waypoint = waypointEditPanel.addChild(new Button("Next >>"));
		button_next_waypoint.setTextHAlignment(HAlignment.Center);
		button_next_waypoint.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				sim.pause(true);
				sim.startNextLeg(true);
			}
		});

		Button button_insert_waypoint = waypointEditPanel.addChild(new Button("Add New"));
		button_insert_waypoint.setTextHAlignment(HAlignment.Center);
		button_insert_waypoint.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {

				// Add new leg to current scan:
				Leg leg = new Leg();

				// Copy scanner settings for new leg from current leg:
				leg.mScannerSettings = new ScannerSettings(mCurrentLegCached.mScannerSettings);

				// Set leg position:
				PlatformSettings ps = new PlatformSettings();

				Vector3D pos = sim.getCurrentLeg().mPlatformSettings.getPosition();
				ps.setPosition(pos);
				leg.mPlatformSettings = ps;
				leg.mPlatformSettings.onGround = sim.getCurrentLeg().mPlatformSettings.onGround;

				int index = sim.getCurrentLegIndex() + 1;
				sim.mSurvey.addLeg(index, leg);
				sim.startLeg(index, true);

				SebEvents.events.fire("survey_changed", null);

				mApp.setActiveTool(mApp.mShowWaypointsAppState);
			}
		});

		Button button_remove_waypoint = waypointEditPanel.addChild(new Button("Delete"));
		button_remove_waypoint.setTextHAlignment(HAlignment.Center);
		button_remove_waypoint.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				sim.removeCurrentLeg();
			}
		});

		Button button_move_waypoint = waypointEditPanel.addChild(new Button("Move"));
		button_move_waypoint.setTextHAlignment(HAlignment.Center);
		button_move_waypoint.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {

				mApp.setActiveTool(mApp.mShowWaypointsAppState);
			}
		});

		mCheckbox_autoground = new Checkbox("Auto-Ground");
		waypointEditPanel.addChild(mCheckbox_autoground);

		mCheckbox_autoground.addClickCommands(new Command<Button>() {

			@Override
			public void execute(Button source) {

				PlatformSettings ps = sim.getCurrentLeg().mPlatformSettings;

				Checkbox cb = (Checkbox) source;
				ps.onGround = cb.isChecked();

				if (ps.onGround) {
					Vector3D ground = sim.getScanner().platform.scene.getGroundPointAt(ps.getPosition());

					if (ground != null) {
						ps.z = ground.getZ();
					}

				}

				sim.getScanner().platform.setPosition(ps.getPosition());
			}

		});
		// ###################### END Set Up Waypoint Edit Panel #####################

		// ############### BEGIN Set up scanner settings panel #################

		Container scannerSettingsPanel = new Container();
		scannerSettingsPanel.setLocalTranslation(0, 800, 0);

		scannerSettingsPanel = myWindow;

		Label label_scanSettings = new Label("SCAN SETTINGS");
		label_scanSettings.setColor(white);
		label_scanSettings.setFontSize(headingSize);

		scannerSettingsPanel.addChild(label_scanSettings);

		Button button_editScanField = scannerSettingsPanel.addChild(new Button("Edit Scan Field"));
		button_editScanField.setTextHAlignment(HAlignment.Center);
		button_editScanField.addClickCommands(new Command<Button>() {
			@Override
			public void execute(Button source) {
				// setEditMode(EditMode.EDIT_SCAN_FIELD);

				mApp.setActiveTool(mApp.mShowScanFieldAppState);
			}
		});

		mCheckbox_scannerActive = new Checkbox("Scanner active");
		mCheckbox_scannerActive.setChecked(sim.getCurrentLeg().mScannerSettings.active);

		waypointEditPanel.addChild(mCheckbox_scannerActive);

		mCheckbox_scannerActive.addClickCommands(new Command<Button>() {

			@Override
			public void execute(Button source) {

				boolean active = sim.getCurrentLeg().mScannerSettings.active;

				sim.getCurrentLeg().mScannerSettings.active = !active;

				Checkbox cb = (Checkbox) source;
				cb.setChecked(!active);

				sim.getScanner().setActive(!active);

				sim.startLeg(sim.getCurrentLegIndex(), true);
			}

		});

		scannerSettingsPanel.addChild(new Label("Head Rot. (deg/sec):"));
		mTextField_headRotatePerSec_deg = new TextField("");
		scannerSettingsPanel.addChild(mTextField_headRotatePerSec_deg);

		scannerSettingsPanel.addChild(new Label("Pulse Freq. (Hz):"));
		mTextField_pulseFreq_hz = new TextField("");
		scannerSettingsPanel.addChild(mTextField_pulseFreq_hz);

		scannerSettingsPanel.addChild(new Label("Scan Freq. (Hz):"));
		mTextField_scanFreq_hz = new TextField("");
		scannerSettingsPanel.addChild(mTextField_scanFreq_hz);

		scannerSettingsPanel.addChild(new Label("Scan Angle (Deg.):"));
		mTextField_scanAngle_deg = new TextField("");
		scannerSettingsPanel.addChild(mTextField_scanAngle_deg);

		Button button_apply = new Button("Apply");
		button_apply.setTextHAlignment(HAlignment.Center);

		scannerSettingsPanel.addChild(button_apply);

		button_apply.addClickCommands(new Command<Button>() {

			@Override
			public void execute(Button source) {

				try {
					double headRotateSpeed_rad = Double.parseDouble(mTextField_headRotatePerSec_deg.getText()) * (Math.PI / 180);
					sim.getCurrentLeg().mScannerSettings.headRotatePerSec_rad = (double) headRotateSpeed_rad;
				} catch (Exception e) {

				}

				try {
					int pulseFreq_hz = Integer.parseInt(mTextField_pulseFreq_hz.getText());
					sim.getCurrentLeg().mScannerSettings.pulseFreq_Hz = pulseFreq_hz;
				} catch (Exception e) {

				}

				try {
					int scanFreq_hz = Integer.parseInt(mTextField_scanFreq_hz.getText());
					sim.getCurrentLeg().mScannerSettings.scanFreq_Hz = scanFreq_hz;
				} catch (Exception e) {

				}

				try {
					double scanAngle_rad = Double.parseDouble(mTextField_scanAngle_deg.getText()) * (Math.PI / 180);
					sim.getCurrentLeg().mScannerSettings.scanAngle_rad = scanAngle_rad;
				} catch (Exception e) {

				}

				sim.startLeg(sim.getCurrentLegIndex(), true);
			}

		});

		// ############### END Set up scanner settings panel #################

		// ################ END Set up Lemur UI ##################

		inputManager.addMapping("add_leg", new KeyTrigger(KeyInput.KEY_INSERT));
		inputManager.addMapping("delete_leg", new KeyTrigger(KeyInput.KEY_DELETE));

		inputManager.addMapping("pgup", new KeyTrigger(KeyInput.KEY_PGUP));
		inputManager.addMapping("pgdown", new KeyTrigger(KeyInput.KEY_PGDN));

		inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_UP));
		inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_DOWN));

		inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_LEFT));
		inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_RIGHT));

		//inputManager.addListener(mActionListener, "delete_leg", "add_leg", "up", "down", "left", "right", "pgup", "pgdown");	
	}

	@Override
	public void update(float tpf) {
		
		// ############### BEGIN Update progress panel #################
		if(!sim.isPaused()){
		
		mProgressBar_survey.setProgressPercent(sim.getProgress() / 100f);
		mProgressBar_leg.setProgressPercent(sim.getLegProgress() / 100f);
		mProgressBar_leg.setMessage("Leg " + (sim.getCurrentLegIndex() + 1) + "/" + sim.getNumEffectiveLegs());
			
		mLabel_surveyElapsed.setText("Elapsed      " + sim.milliToString(sim.getElapsedTime()));
		mLabel_surveyRemaining.setText("Remaining " + sim.milliToString(sim.getRemaningTime()));
		mLabel_legElapsed.setText("Elapsed      " + sim.milliToString(sim.getLegElapsedTime()));
		mLabel_legRemaining.setText("Remaining " + sim.milliToString(sim.getLegRemaningTime()));
		}
		// ################# END Update progress panel ################
		
		Leg leg = sim.getCurrentLeg();

		ScannerSettings ss = leg.mScannerSettings;

		if (leg != mCurrentLegCached) {

			mCurrentLegCached = leg;

			mCheckbox_autoground.setChecked(sim.getCurrentLeg().mPlatformSettings.onGround);

			// ############### BEGIN Update Scan Settings Panel #################

			mTextField_headRotatePerSec_deg.setText(String.valueOf(ss.headRotatePerSec_rad * (180 / Math.PI)));
			mTextField_pulseFreq_hz.setText(String.valueOf(ss.pulseFreq_Hz));
			mTextField_scanFreq_hz.setText(String.valueOf(ss.scanFreq_Hz));
			mTextField_scanAngle_deg.setText(String.valueOf(ss.scanAngle_rad * (180 / Math.PI)));

			mCheckbox_scannerActive.setChecked(leg.mScannerSettings.active);

		}
		// ############### BEGIN Update Scan Settings Panel #################

		// ################# BEGIN Display platform coordinates ################
		Vector3D platformPos = sim.getScanner().platform.getPosition();

		Vector3D ground = sim.getScanner().platform.scene.getGroundPointAt(sim.getScanner().platform.getPosition());

		double agl = 0;
		if (ground != null) {
			agl = platformPos.getZ() - ground.getZ();
		}

		String line = String.format("Pos: %.2f / %.2f / %.2f / AGL: %.2f", platformPos.getX(), platformPos.getY(), platformPos.getZ(), agl);

		mLabel_platformPos.setText(line);
		// ################# END Display platform coordinates ################
	}

	@Override
	public void handleEvent(String eventName, Object payload) {

		switch (eventName) {
		case "simulation_pause_state_changed":

			mButton_sim_pause.setText(sim.isPaused() ? "Play" : "Pause");

			break;
		}
	}

}
