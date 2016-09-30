package de.uni_hd.giscience.helios.visualization.appStates;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.ChaseCamera;
import com.jme3.input.FlyByCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;

import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;

public class CameraAppState extends BaseAppState {

	private double cfg_camSpeedAccel = 1.08;
	// ######### BEGIN Camera ###########
	private double camSpeed = 5;
	private double maxCamSpeed = 0;
	private boolean accelerate = false;
	// ######### END Camera ###########

	private boolean isPressed_w = false;
	private boolean isPressed_a = false;
	private boolean isPressed_s = false;
	private boolean isPressed_d = false;

	// ##### BEGIN Action listeners ######
	private ActionListener actionListener = new ActionListener() {

		public void onAction(String name, boolean keyPressed, float tpf) {

			if (keyPressed) {

				if (name.equals("W"))
					isPressed_w = true;
				if (name.equals("A"))
					isPressed_a = true;
				if (name.equals("S"))
					isPressed_s = true;
				if (name.equals("D"))
					isPressed_d = true;

				if (name.equals("W") || name.equals("A") || name.equals("S") || name.equals("D")) {

					if (camSpeed < maxCamSpeed) {
						accelerate = true;
					}

					mApp.getFlyByCamera().setMoveSpeed((int) Math.min(camSpeed, maxCamSpeed));
				}
			}

			else {
				if (name.equals("W"))
					isPressed_w = false;
				if (name.equals("A"))
					isPressed_a = false;
				if (name.equals("S"))
					isPressed_s = false;
				if (name.equals("D"))
					isPressed_d = false;
			}

			if (!isPressed_w && !isPressed_a && !isPressed_s && !isPressed_d) {
				accelerate = false;
				camSpeed = 5;

				mApp.getFlyByCamera().setMoveSpeed((int) Math.min(camSpeed, maxCamSpeed));
			}

			if (name.equals("Camera") && !keyPressed) {
				mApp.setCameraMode();

			}

			
		}
	};

	public CameraAppState(SurveyPlayback sim) {
		super(sim);

	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		FlyByCamera flyCam = this.mApp.getFlyByCamera();

		flyCam.setEnabled(false);
		flyCam.setDragToRotate(true);
		flyCam.setMoveSpeed(0);

		Vector3D bs = sim.getScanner().platform.scene.getAABB().getSize();
		maxCamSpeed = Math.max(Math.max(bs.getX(), bs.getY()), bs.getZ()) * 0.4;

		if (maxCamSpeed < 2) {
			maxCamSpeed = 2;
		}

		inputManager.addMapping("Camera", new KeyTrigger(KeyInput.KEY_V));
		inputManager.addMapping("W", new KeyTrigger(KeyInput.KEY_W));
		inputManager.addMapping("A", new KeyTrigger(KeyInput.KEY_A));
		inputManager.addMapping("S", new KeyTrigger(KeyInput.KEY_S));
		inputManager.addMapping("D", new KeyTrigger(KeyInput.KEY_D));
		inputManager.addMapping("E", new KeyTrigger(KeyInput.KEY_E));

		// Add the names to the action listener.
		inputManager.addListener(actionListener, "Camera", "W", "A", "S", "D", "E");

	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

	}

	@Override
	public void update(float tpf) {

		// Set camera speed:
		if (accelerate) {
			camSpeed *= cfg_camSpeedAccel;
			mApp.getFlyByCamera().setMoveSpeed((int) Math.min(camSpeed, maxCamSpeed));
		}
	}

}
