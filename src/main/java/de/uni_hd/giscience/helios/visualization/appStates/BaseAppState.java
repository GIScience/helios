package de.uni_hd.giscience.helios.visualization.appStates;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;
import de.uni_hd.giscience.helios.visualization.JMEFrontEnd;

public class BaseAppState extends AbstractAppState {

	JMEFrontEnd mApp;

	InputManager inputManager;
	AssetManager mAssetManager;
	Node rootNode;
	Node mWorldNode = null;

	boolean mToolEnabled = false;
	
	SurveyPlayback sim = null;
	
	SurveyPlayback mPlayback = null;
	
	boolean enabled = false;

	public BaseAppState(SurveyPlayback sim) {
		super();
		this.sim = sim;
		
		this.mPlayback = (SurveyPlayback) sim;
	}

	@Override
	public void initialize(AppStateManager stateManager, Application aapp) {
		super.initialize(stateManager, aapp);

		this.mApp = (JMEFrontEnd) aapp;

		mAssetManager = this.mApp.getAssetManager();
		inputManager = this.mApp.getInputManager();
		rootNode = this.mApp.getRootNode();

		mWorldNode = (Node) rootNode.getChild("world");
	}

	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

		this.enabled = enabled;
	}
	
	// #### BEGIN Utility methods to convert Apache Math objects to their JME equivalents ###
	Vector3f am2jme_vector(Vector3D vecSim) {

		if (vecSim == null)
			return null;
		return new Vector3f((float) vecSim.getX(), (float) vecSim.getY(), (float) vecSim.getZ());
	}

	Quaternion am2jme_rotation(Rotation r) {

		if (r == null)
			return null;

		Quaternion q = new Quaternion();
		q.fromAngleAxis((float) r.getAngle(), am2jme_vector(r.getAxis()));
		return q;
	}
	
	public void setToolEnabled(boolean enabled) {
		mToolEnabled = enabled;
	}
	
	// #### END Utility methods to convert Apache Math objects to their JME equivalents ###
}
