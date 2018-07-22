// TODO 3: Replace build-in flyCam AppState with custom free camera movement AppState


// TODO 2: Display simulation speed

// TODO 4: Move sky and lighting stuff to own app state?

package de.uni_hd.giscience.helios.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.vecmath.Color4f;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.StyleLoader;

import de.uni_hd.giscience.helios.core.scene.Scene;
import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;
import de.uni_hd.giscience.helios.visualization.appStates.BaseAppState;
import de.uni_hd.giscience.helios.visualization.appStates.CameraAppState;
import de.uni_hd.giscience.helios.visualization.appStates.ToolbarAppState;
import de.uni_hd.giscience.helios.visualization.appStates.ShowPlatformAndScannerAppState;
import de.uni_hd.giscience.helios.visualization.appStates.ShowPointCloudAppState;
import de.uni_hd.giscience.helios.visualization.appStates.ShowScannedSceneAppState;
import de.uni_hd.giscience.helios.visualization.appStates.EditScanFieldAppState;
import de.uni_hd.giscience.helios.visualization.appStates.EditWaypointsAppState;
import sebevents.EventListener;

public class JMEFrontEnd extends SimpleApplication implements EventListener {

	boolean cursorEnabled = false;

	public BaseAppState mActiveTool = null;

	public ChaseCamera chaseCam = null;

	int cameraMode = 1;
	// ############ BEGIN Configuration #############
	private int cfg_shadowMode = 1;
	final int cfg_shadowMapSize = 4096;

	// Number of shadow maps (between 1 and 4, higher number = higher quality but lower FPS)
	int cfg_shadow_split = 1;
	// ############ END Configuration #############

	public SurveyPlayback sim = null;

	// ######### BEGIN AppStates #########
	CameraAppState mCameraAppState = null;
	ShowPointCloudAppState mShowPointCloudAppState = null;
	ShowScannedSceneAppState mShowScannedSceneAppState = null;
	public EditScanFieldAppState mShowScanFieldAppState = null;
	ShowPlatformAndScannerAppState mShowPlatformAndScannerAppState = null;
	public EditWaypointsAppState mShowWaypointsAppState = null;
	ToolbarAppState mToolbarAppState = null;
	// ######### END AppStates #########

	public AppSettings mSettings = null;

	ColorRGBA convertColor(Color4f col) {

		if (col != null) {
			return new ColorRGBA(col.x, col.y, col.z, col.w);
		}

		return null;
	}

	// ##### BEGIN Action listeners ######
	private ActionListener actionListener = new ActionListener() {

		public void onAction(String name, boolean keyPressed, float tpf) {

			if (keyPressed) {

			}

		}
	};

	// ##### END Action listeners ######

	public Node getGuiNode() {
		return guiNode;
	}

	public void init(SurveyPlayback sim) {

		mSettings = new AppSettings(true);

		mSettings.setTitle("HELIOS - The Heidelberg LiDAR Operations Simulator");
		mSettings.setVSync(true);
		mSettings.setResolution(1680, 1050);
		mSettings.setResolution(1280, 1024);
		mSettings.setResolution(1600, 1024);
		// setting.setResolution(1024,768);
		mSettings.setResolution(1600, 900);
        
		mSettings.setSamples(2);

		setSettings(mSettings);

		setShowSettings(true);

		// ATTENTION: This is REQUIRED to prevent freezing of the whole computer if the program loses focus!
		// setPauseOnLostFocus() must be "false" since currently, setting it to "true" won't stop the actual simulation anyway.
		setPauseOnLostFocus(false);
		this.sim = sim;
	}

	@Override
	public void simpleInitApp() {

		GuiGlobals.initialize(this);

		// Load the 'glass' style
		// BaseStyles.loadGlassStyle();

		// Set 'glass' as the default style when not specified
		GuiGlobals.getInstance().getStyles().setDefaultStyle("helios");

		File styleFile = new File("style.groovy");
		try {
			new StyleLoader().loadStyle(styleFile.toString(), new FileReader(styleFile));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		setDisplayFps(false);
		setDisplayStatView(false);

		/*
		 * FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
		 * 
		 * BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects); bloom.setBloomIntensity(3); fpp.addFilter(bloom); viewPort.addProcessor(fpp);
		 */

		// Register sceneparts folder in asset manager:
		assetManager.registerLocator("assets", FileLocator.class);
		assetManager.registerLocator("data", FileLocator.class);


		cam.setFrustumFar(100000);

		// TODO 4: Find out wheterh this can be used to avoid rotation of the scene to fix z <-> y poblem
		// cam.setAxes(left, up, up);

		Node worldNode = new Node("world");
		rootNode.attachChild(worldNode);

		// ############ BEGIN Create and attach app states #################

		mShowPointCloudAppState = new ShowPointCloudAppState(sim);
		stateManager.attach(mShowPointCloudAppState);

		mShowScannedSceneAppState = new ShowScannedSceneAppState(sim);
		stateManager.attach(mShowScannedSceneAppState);

		mShowPlatformAndScannerAppState = new ShowPlatformAndScannerAppState(sim);
		stateManager.attach(mShowPlatformAndScannerAppState);

		// Experimental app states:

		mCameraAppState = new CameraAppState(sim);
		stateManager.attach(mCameraAppState);

		mShowWaypointsAppState = new EditWaypointsAppState(sim);
		stateManager.attach(mShowWaypointsAppState);

		mShowScanFieldAppState = new EditScanFieldAppState(sim);
		stateManager.attach(mShowScanFieldAppState);

		// Menu bar app state:
		mToolbarAppState = new ToolbarAppState(sim);
		stateManager.attach(mToolbarAppState);

		// ############ END Create and attach app states #################

		// Rotate the entire world so that z axis is "up":
		worldNode.rotate((float) -(Math.PI / 2), 0.0f, 0.0f);
		worldNode.rotate(0f, 0f, (float) -(Math.PI / 2));

		// ############## BEGIN Add sun ####################
		DirectionalLight sun = new DirectionalLight();
		// TODO 4: Read sun color from scene
		sun.setColor(new ColorRGBA(1, 1, 1, 1));

		Vector3D sd = sim.getScanner().platform.scene.sunDir;

		Vector3f sunDir = new Vector3f((float) sd.getX(), (float) sd.getZ(), (float) -sd.getY()).normalize();
		sun.setDirection(sunDir);
		worldNode.addLight(sun);
		// ############## END Add sun ####################

		// ############## BEGIN Add Sun Shadows ###############
		if (cfg_shadowMode == 1) {
			DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, cfg_shadowMapSize, cfg_shadow_split);
			dlsr.setLight(sun);
			viewPort.addProcessor(dlsr);
		} else if (cfg_shadowMode == 2) {
			DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, cfg_shadowMapSize, cfg_shadow_split);
			dlsf.setLight(sun);
			dlsf.setEnabled(true);

			FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
			fpp.addFilter(dlsf);
			viewPort.addProcessor(fpp);
		}
		// ############## END Add Sun Shadows ###############

		// ############## BEGIN Add ambient light ####################

		AmbientLight al = new AmbientLight();

		// TODO 4: Read ambient color from scene
		al.setColor(ColorRGBA.White);
		worldNode.addLight(al);

		// ############## END Add ambient light ####################

		// ############# BEGIN Set background color ###############
		ColorRGBA skyColor = convertColor(sim.getScanner().platform.scene.skyColor);

		if (skyColor == null) {
			skyColor = new ColorRGBA(0.5f, 0.5f, 0.9f, 1.0f);
		}

		viewPort.setBackgroundColor(skyColor);
		// ############# END Set background color ###############

		// ################# BEGIN Add Skybox ################
		Scene scene = sim.getScanner().platform.scene;

		try {
			// TODO 4: Implement more robust check for file/folder existence
			if (!scene.skyboxTexturesFolder.equals("")) {
				// assetManager.registerLocator(scene.skyboxTexturesFolder, FileLocator.class);

				Texture sky_up = assetManager.loadTexture(scene.skyboxTexturesFolder + "/up.jpg");
				Texture sky_down = assetManager.loadTexture(scene.skyboxTexturesFolder + "/down.jpg");
				Texture sky_north = assetManager.loadTexture(scene.skyboxTexturesFolder + "/north.jpg");
				Texture sky_south = assetManager.loadTexture(scene.skyboxTexturesFolder + "/south.jpg");
				Texture sky_east = assetManager.loadTexture(scene.skyboxTexturesFolder + "/east.jpg");
				Texture sky_west = assetManager.loadTexture(scene.skyboxTexturesFolder + "/west.jpg");

				Spatial sky = SkyFactory.createSky(assetManager, sky_west, sky_east, sky_north, sky_south, sky_up, sky_down);
				sky.rotate(0, scene.skyboxAzimuth_rad, 0);
				rootNode.attachChild(sky);
			}
		} catch (Exception e) {
		}
		// ################# END Add Skybox ################

		// Set up keyboard mapping:
		inputManager.addMapping("Q", new KeyTrigger(KeyInput.KEY_Q));
		inputManager.addMapping("G", new KeyTrigger(KeyInput.KEY_G));

		inputManager.addMapping("tool_edit_waypoints", new KeyTrigger(KeyInput.KEY_F1));
		inputManager.addMapping("tool_edit_settings", new KeyTrigger(KeyInput.KEY_F2));
		inputManager.addMapping("tool_simcontrol", new KeyTrigger(KeyInput.KEY_F3));
		inputManager.addMapping("save", new KeyTrigger(KeyInput.KEY_F4));

		// Unbind the "Esc" key from closing the JME window:
		inputManager.deleteMapping(INPUT_MAPPING_EXIT);

		// Add the names to the action listener.
		inputManager.addListener(actionListener, "tool_edit_settings", "tool_edit_waypoints", "tool_simcontrol", "G", "Q", "save");
	}

	public void setCameraMode() {

		cameraMode = ++cameraMode % 2;

		if (chaseCam == null) {
			chaseCam = new ChaseCamera(getCamera(), rootNode.getChild("mountNode"), inputManager);
			// chaseCam = new ChaseCamera(cam, inputManager);
			chaseCam.setTrailingEnabled(false);
			chaseCam.setMaxDistance(200);
			chaseCam.setSmoothMotion(false);

		}

		if (cameraMode == 0) {
			getFlyByCamera().setEnabled(true);
			chaseCam.setEnabled(false);

		} else if (cameraMode == 1) {
			getFlyByCamera().setEnabled(false);
			chaseCam.setEnabled(true);
			chaseCam.setSpatial(rootNode.getChild("mountNode"));
		}
	}

	@Override
	public void simpleUpdate(float tpf) {

	}

	@Override
	public void handleEvent(String eventName, Object payload) {


	}

	public void setActiveTool(BaseAppState tool) {

		if (mActiveTool != null) {
			mActiveTool.setToolEnabled(false);
		}

		mActiveTool = tool;

		if (mActiveTool != null) {
			mActiveTool.setToolEnabled(true);
		}
	}
}
