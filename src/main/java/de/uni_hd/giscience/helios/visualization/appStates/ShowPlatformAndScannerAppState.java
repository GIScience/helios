package de.uni_hd.giscience.helios.visualization.appStates;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;

import de.uni_hd.giscience.helios.core.platform.Platform;
import de.uni_hd.giscience.helios.core.scanner.Measurement;
import de.uni_hd.giscience.helios.core.scanner.Scanner;
import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;

public class ShowPlatformAndScannerAppState extends BaseAppState {

	boolean showHoriScanIndicator = false;

	SurveyPlayback playback;

	private int cfg_laserBeamWidth_pixels = 2;

	// ############ BEGIN Platform & Scanner ############
	private Geometry beamGeometry;

	private Node platformNode = null;
	public Node mountNode = null;
	private Node scannerNode = null;
	private Node emitterNode = null;

	private Spatial platformModel = null;
	private Spatial scannerModel = null;

	String platformModelPath = "";
	String scannerModelPath = "";

	Platform currentPlatform = null;
	Scanner mCurrentScanner = null;

	// ############ END Platform & Scanner ############

	public ShowPlatformAndScannerAppState(SurveyPlayback sim) {
		super(sim);
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		playback = (SurveyPlayback) sim;

		// ################## BEGIN Add platform, scanner and emitter nodes #################
		// Add platform node:
		platformNode = new Node("platformNode");
		mWorldNode.attachChild(platformNode);

		// Add mount node:
		mountNode = new Node("mountNode");
		platformNode.attachChild(mountNode);

		// Add scammer node:
		scannerNode = new Node("scannerNode");
		mountNode.attachChild(scannerNode);

		// Add emitter node:
		emitterNode = new Node("emitterNode");
		scannerNode.attachChild(emitterNode);

		if (mApp.chaseCam == null) {
			mApp.chaseCam = new ChaseCamera(mApp.getCamera(), rootNode.getChild("mountNode"), inputManager);
			// chaseCam = new ChaseCamera(cam, inputManager);
			mApp.chaseCam.setTrailingEnabled(false);
			mApp.chaseCam.setMaxDistance(200);
			mApp.chaseCam.setSmoothMotion(true);
		}
		// ################## END Add platform, scanner and emitter nodes #################

		// ############################ BEGIN Add laser beam geometry ################################

		Mesh beamMesh = new Mesh();

		beamMesh.setMode(Mesh.Mode.Lines);
		beamMesh.setLineWidth(cfg_laserBeamWidth_pixels);
		beamMesh.setBuffer(VertexBuffer.Type.Position, 3, new float[] { 0, 0, 0, 0, 1, 0 });
		beamMesh.setBuffer(VertexBuffer.Type.Index, 2, new short[] { 0, 1 });
		beamMesh.updateBound();
		beamMesh.updateCounts();

		beamGeometry = new Geometry("beam", beamMesh);

		Material beamMaterial = new Material(mAssetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		beamMaterial.setColor("Color", new ColorRGBA(255, 0, 0, 0));
		// lineMaterial.setColor("GlowColor", ColorRGBA.Red);

		beamGeometry.setMaterial(beamMaterial);

		emitterNode.attachChild(beamGeometry);

		// Initialize beam geometry with zero y scale to make it invisible before the scanner is started:
		beamGeometry.setLocalScale(1, 0, 1);

		// ############################### END Add laser beam geometry ############################

	}

	@Override
	public void update(float tpf) {

		// ################ BEGIN What happens when there is a new scanner ################
		if (sim.getScanner() != mCurrentScanner) {
			System.out.println("Scanner changed!");

			mCurrentScanner = sim.getScanner();

			// ######### BEGIN Replace scanner model if necessary ############
			if (scannerModel != null) {
				scannerNode.detachChild(scannerModel);
			}

			scannerModel = null;

			if (mCurrentScanner != null) {

				if (!mCurrentScanner.cfg_device_visModelPath.equals("")) {
					scannerModel = mAssetManager.loadModel("Models/" + mCurrentScanner.cfg_device_visModelPath);
					scannerModel.setShadowMode(ShadowMode.Cast);
					scannerNode.attachChild(scannerModel);
				}

				// Set emitter position and attitude:

				emitterNode.setLocalTranslation(am2jme_vector(mCurrentScanner.cfg_device_headRelativeEmitterPosition));
				emitterNode.setLocalRotation(am2jme_rotation(mCurrentScanner.cfg_device_headRelativeEmitterAttitude));

				// ################ BEGIN What happens when there is a new platform ################
				if (mCurrentScanner.platform != currentPlatform) {

					System.out.println("Playback: Platform changed!");

					currentPlatform = sim.getScanner().platform;

					// ######### BEGIN Replace platform 3D model if necessary #############
					if (platformModel != null) {
						platformNode.detachChild(platformModel);
					}

					platformModel = mAssetManager.loadModel("Models/" + currentPlatform.cfg_device_visModelPath);
					platformModel.setShadowMode(ShadowMode.Cast);
					platformNode.attachChild(platformModel);
					// ######### END Replace platform 3D model if necessary #############

					// Set mount position and attitude:
					mountNode.setLocalTranslation(am2jme_vector(currentPlatform.cfg_device_relativeMountPosition));
					mountNode.setLocalRotation(am2jme_rotation(currentPlatform.cfg_device_relativeMountAttitude));
				}
				// ################ END What happens when there is a new platform ################

			}
			// ######### END Replace scanner model if necessary ############
		}
		// ################ END What happens when there is a new scanner ################

		// ################## BEGIN Update laser beam ######################

		if (mCurrentScanner != null) {

			// Update laser beam attitude:
			beamGeometry.setLocalRotation(am2jme_rotation(mCurrentScanner.beamDeflector.getEmitterRelativeAttitude()));

			// Update laser beam length:
			float beamLength = 10000;

			if (sim.isPaused() || !mCurrentScanner.isActive() || !mCurrentScanner.beamDeflector.lastPulseLeftDevice()) {
				beamLength = 0;
			} else {
				int lastMeasurementIndex = sim.mbuffer.getLastRecordedPointIndex();

				Measurement lastPoint = sim.mbuffer.getEntryAt(lastMeasurementIndex);

				if (mCurrentScanner.lastPulseWasHit() && lastPoint != null) {
					beamLength = (float) lastPoint.distance;
				} else {
					beamLength = 10000;
				}
			}

			beamGeometry.setLocalScale(1, beamLength, 1);
			// ################## END Update laser beam ######################

			// ################ BEGIN Update platform model ################
			// Update platform position and attitude:
			platformNode.setLocalTranslation(am2jme_vector(currentPlatform.getPosition()));
			platformNode.setLocalRotation(am2jme_rotation(currentPlatform.getAttitude()));
			// ################ END Update platform model ################

			// ############### BEGIN Update scanner ###############
			// Update emitter attitude:

			// TODO 4: Direction of laser pulse and length of beam is not synchronized here, because:
			// For beam direciton, the current scanner state is used. However, for beam length,
			// the coordinates of the last captured point are used. In most situations, these will not match,
			// since pulses are simulated asynchronously by multiple threads. sbecht 2016-02-10

			scannerNode.setLocalRotation(am2jme_rotation(mCurrentScanner.scannerHead.getMountRelativeAttitude()));
			// ############### END Update scanner ###############
		} else {
			System.out.println("Current scanner is null!");
		}
	}

}
