package de.uni_hd.giscience.helios.visualization.appStates;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;

import de.uni_hd.giscience.helios.surveyplayback.Leg;
import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;
import sebevents.SebEvents;

public class EditWaypointsAppState extends BaseAppState implements sebevents.EventListener {

	Spatial mMoveCursorModel = null;

	Node mLegMarkersGroup = null;

	double mLegMoveStep = 0.1;

	boolean mMoveForward = false;
	boolean mMoveBackward = false;
	boolean mMoveLeft = false;
	boolean mMoveRight = false;

	double maxMoveSpeedH = 20f;
	double mMaxMoveSpeedV = 8f;

	double mMoveSpeedVertical = 0.1f;
	double mMoveSpeedHorizontal = 0.1f;

	int mMoveUpDown = 0;

	// ##### BEGIN Action listeners ######
	private ActionListener mActionListener = new ActionListener() {

		public void onAction(String name, boolean keyPressed, float tpf) {

			if (!mToolEnabled) {
				return;
			}

			if (keyPressed) {

				switch (name) {

				case "up":
					mMoveForward = true;
					break;

				case "down":
					mMoveBackward = true;
					break;

				case "right":
					mMoveRight = true;
					break;

				case "left":
					mMoveLeft = true;
					break;

				case "pgup":
					mMoveUpDown = 1;
					break;

				case "pgdown":
					mMoveUpDown = -1;
					break;
				}
			}

			// Key released:
			else {
				switch (name) {

				case "up":
					mMoveForward = false;
					break;

				case "down":
					mMoveBackward = false;
					break;

				case "right":
					mMoveRight = false;
					break;

				case "left":
					mMoveLeft = false;
					break;

				case "pgup":
					mMoveUpDown = 0;
					break;

				case "pgdown":
					mMoveUpDown = 0;
					break;
				}
			}
		}
	};

	public EditWaypointsAppState(SurveyPlayback sim) {
		super(sim);
		mLegMarkersGroup = new Node("legMarkers");

		SebEvents.events.addListener("survey_changed", this);
		SebEvents.events.addListener("simulation_pause_state_changed", this);
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		mWorldNode.attachChild(mLegMarkersGroup);

		mMoveCursorModel = mAssetManager.loadModel("Models/movecursor.obj");
		mMoveCursorModel.setName("moveCursor");
		mMoveCursorModel.setLocalTranslation(0, 0, 1f);
		mMoveCursorModel.setCullHint(CullHint.Always);
		
		updateSceneGraph();

		Vector3D bs = sim.getScanner().platform.scene.getAABB().getSize();
		maxMoveSpeedH = Math.max(bs.getX(), bs.getY()) * 0.003;

		inputManager.addListener(mActionListener, "up", "down", "left", "right", "pgup", "pgdown");
	}

	@Override
	public void setToolEnabled(boolean enabled) {

		if (enabled) {
			sim.pause(true);
		}

		if (mMoveCursorModel != null) {
			if (enabled) {
				mMoveCursorModel.setCullHint(CullHint.Inherit);
			} else {
				mMoveCursorModel.setCullHint(CullHint.Always);
			}
		}

		super.setToolEnabled(enabled);
	}

	@Override
	public void update(float tpf) {

		// ############ BEGIN Update leg marker mesh positions ############
		for (Spatial legMarkerNode : mLegMarkersGroup.getChildren()) {

			Node node = (Node) legMarkerNode;
			int legIndex = legMarkerNode.getUserData("legIndex");

			Leg leg = mPlayback.mSurvey.legs.get(legIndex);

			if (leg != null) {

				// ###### BEGIN Update leg marker position #######
				float x = (float) leg.mPlatformSettings.x;
				float y = (float) leg.mPlatformSettings.y;
				float z = (float) leg.mPlatformSettings.z;

				Vector3f pos = new Vector3f(x, y, z);

				legMarkerNode.setLocalTranslation(pos);
				// ###### END Update leg marker position #######

				// ####### BEGIN Update leg marker visibility ########

				Spatial markerModel = node.getChild("markerModel");

				if (leg == mPlayback.getCurrentLeg()) {

					markerModel.setCullHint(CullHint.Always);

					if (mMoveCursorModel.getParent() != legMarkerNode) {

						node.attachChild(mMoveCursorModel);
					}
				} else {
					markerModel.setCullHint(CullHint.Inherit);
				}

				// ####### END Update leg marker visibility ########
			}
		}

		// ############ END Update leg marker mesh positions ############

		// #################### BEGIN Horizontal Waypoint Movement ##################

		Leg leg = sim.getCurrentLeg();

		Vector3f camDirection = mApp.getCamera().getLeft();
		Vector3f camRight = mApp.getCamera().getDirection();

		boolean updatePos = false;

		boolean accelerateH = false;

		if (mMoveBackward) {
			leg.mPlatformSettings.y += camDirection.getZ() * mMoveSpeedHorizontal;
			leg.mPlatformSettings.x -= camDirection.getX() * mMoveSpeedHorizontal;
			updatePos = true;
			accelerateH = true;
		}

		if (mMoveForward) {
			leg.mPlatformSettings.y -= camDirection.getZ() * mMoveSpeedHorizontal;
			leg.mPlatformSettings.x += camDirection.getX() * mMoveSpeedHorizontal;
			updatePos = true;
			accelerateH = true;
		}

		if (mMoveLeft) {
			leg.mPlatformSettings.x -= camRight.getX() * mMoveSpeedHorizontal;
			leg.mPlatformSettings.y += camDirection.getX() * mMoveSpeedHorizontal;
			updatePos = true;
			accelerateH = true;
		}

		if (mMoveRight) {
			leg.mPlatformSettings.x += camRight.getX() * mMoveSpeedHorizontal;
			leg.mPlatformSettings.y -= camDirection.getX() * mMoveSpeedHorizontal;
			updatePos = true;
			accelerateH = true;
		}

		if (accelerateH) {
			mMoveSpeedHorizontal *= 1.05;

			if (mMoveSpeedHorizontal > maxMoveSpeedH) {
				mMoveSpeedHorizontal = maxMoveSpeedH;
			}
		} else {
			mMoveSpeedHorizontal = 0.1f;
		}
		// #################### END Horizontal Waypoint Movement ##################

		// ############# BEGIN Vertical Waypoint Movement ###############
		if (mMoveUpDown != 0) {
			leg.mPlatformSettings.z += Math.signum(mMoveUpDown) * mMoveSpeedVertical;

			updatePos = true;

			mMoveSpeedVertical *= 1.05;

			if (mMoveSpeedVertical > mMaxMoveSpeedV) {
				mMoveSpeedVertical = mMaxMoveSpeedV;
			}
		} else {
			mMoveSpeedVertical = 0.05f;
		}
		// ############# END Vertical Waypoint Movement ###############

		sim.getScanner().platform.applySettings(leg.mPlatformSettings, false);

		if (updatePos) {

			if (leg.mPlatformSettings.onGround) {
				Vector3D ground = sim.getScanner().platform.scene.getGroundPointAt(leg.mPlatformSettings.getPosition());

				if (ground != null) {
					leg.mPlatformSettings.z = ground.getZ();
				}

			}

			sim.getScanner().platform.setPosition(leg.mPlatformSettings.getPosition());
		}

	}

	public void updateSceneGraph() {

		mLegMarkersGroup.detachAllChildren();

		// ######################## BEGIN Iterate over legs and add marker meshes #######################
		int legIndex = 0;

		for (Leg leg : mPlayback.mSurvey.legs) {

			Node legMarkerNode = new Node();
			legMarkerNode.setUserData("legIndex", legIndex);
			mLegMarkersGroup.attachChild(legMarkerNode);

			Spatial legMarkerModel = mAssetManager.loadModel("Models/misc/scanPosMarker.obj");
			legMarkerModel.setShadowMode(ShadowMode.CastAndReceive);
			legMarkerModel.setName("markerModel");

			legIndex++;

			legMarkerNode.attachChild(legMarkerModel);
			legMarkerNode.attachChild(mMoveCursorModel);
		}
		// ######################## END Iterate over legs and add marker meshes #######################

	}

	@Override
	public void handleEvent(String eventName, Object payload) {

		switch (eventName) {

		case "survey_changed":
			updateSceneGraph();
			break;
		}
	}

}
