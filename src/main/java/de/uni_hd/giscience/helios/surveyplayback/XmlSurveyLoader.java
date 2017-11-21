package de.uni_hd.giscience.helios.surveyplayback;

import javax.vecmath.Color4f;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.uni_hd.giscience.helios.assetsloading.XmlAssetsLoader;
import de.uni_hd.giscience.helios.core.platform.Platform;
import de.uni_hd.giscience.helios.core.scanner.FWFSettings;
import de.uni_hd.giscience.helios.core.scanner.Scanner;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;
import de.uni_hd.giscience.helios.core.scene.Scene;

public class XmlSurveyLoader extends XmlAssetsLoader {

	public XmlSurveyLoader(String filePath) {
		super(filePath);
	}

	Leg createLegFromXML(Element legNode) {

		Leg leg = new Leg();

		// ################# BEGIN Read Platform Settings ###################
		Element platformSettingsNode = (Element) legNode.getElementsByTagName("platformSettings").item(0);

		if (platformSettingsNode != null) {
			leg.mPlatformSettings = createPlatformSettingsFromXml(platformSettingsNode);
		}
		// ################# END Read Platform Settings ###################

		// ################# BEGIN Read Scanner Settings ###################
		Element scannerSettingsNode = (Element) legNode.getElementsByTagName("scannerSettings").item(0);

		if (scannerSettingsNode != null) {
			leg.mScannerSettings = createScannerSettingsFromXml(scannerSettingsNode);
		}
		else {
			leg.mScannerSettings = new ScannerSettings();
		}
		// ################# END Read Scanner Settings ###################

		return leg;
	}

	public Survey createSurveyFromXml(Element surveyNode) {

		Survey survey = new Survey();

		survey.name = (String) getAttribute(surveyNode, "name", String.class, xmlDocFilename);
		survey.sourceFilePath = xmlDocFilePath;

		//String defaultScannerSettingsLocation = (String) getAttribute(surveyNode, "defaultScannerSettings", String.class, "default");


		String scannerAssetLocation = (String) getAttribute(surveyNode, "scanner", String.class, null);

		survey.scanner = (Scanner) getAssetByLocation("scanner", scannerAssetLocation);

		String platformAssetLocation = (String) getAttribute(surveyNode, "platform", String.class, null);
		Platform platform = (Platform) getAssetByLocation("platform", platformAssetLocation);

		survey.scanner.platform = platform;

		// FWF info
		Element scannerFWFSettingsNode = (Element) surveyNode.getElementsByTagName("FWFSettings").item(0);
		survey.scanner.applySettingsFWF((FWFSettings)createFWFSettingsFromXml(scannerFWFSettingsNode));
		 
		// temp backward compatibility
		if(survey.scanner.cfg_device_beamDivergence_rad>0) 
			survey.scanner.FWF_settings.beamDivergence_rad=survey.scanner.cfg_device_beamDivergence_rad;

		if(survey.scanner.getPulseLength_ns()>0) 
			survey.scanner.FWF_settings.pulseLength_ns=survey.scanner.getPulseLength_ns();
		
		// #################### BEGIN Read point cloud color ####################

		Color4f col = createColorFromXml((Element) surveyNode.getElementsByTagName("pointCloudColor").item(0));

		if (col != null) {
			// survey.pointCloudColor = col;
		}

		// #################### END Read point cloud color ####################

		// ##################### BEGIN Read misc parameters ##################
		// Read number of runs:
		survey.numRuns = (int) getAttribute(surveyNode, "numRuns", Integer.class, 1);

		// ######### BEGIN Set initial sim speed factor ##########
		Double speed = (Double) getAttribute(surveyNode, "simSpeed", Double.class, 1.0);
		if (speed <= 0) {
			System.out.println("XML Survey Playback Loader: ERROR: Sim speed can't be <= 0. Setting it to 1.");
			speed = 1d;
		}

		survey.simSpeedFactor = 1.0 / speed;
		// ######### END Set initial sim speed factor ##########

		NodeList legNodes = surveyNode.getChildNodes();

		Vector3D origin = new Vector3D(0, 0, 0);

		for (int kk = 0; kk < legNodes.getLength(); kk++) {

			if (!(legNodes.item(kk) instanceof Element)) {
				continue;
			}

			Element legNode = (Element) legNodes.item(kk);

			// Read leg:
			if (legNode.getTagName().equals("leg")) {

				Leg leg = createLegFromXML(legNode);

				// Deprecated:
				/*
				if (leg.mScannerSettings == null) {
					System.out.println("Applying default scanner settings");
					leg.mScannerSettings = new ScannerSettings(survey.mDefaultScannerSettings);
				}
				*/

				// Add origin shift to waypoint coordinates:
				if (leg.mPlatformSettings != null && origin != null) {
					leg.mPlatformSettings.setPosition(leg.mPlatformSettings.getPosition().add(origin));
				}

				survey.legs.add(leg);
			}

			// ########### BEGIN Read Coordinate System Origin ############
			else if (legNode.getTagName().equals("origin")) {
				origin = createVec3dFromXml(legNode, "");
			}
			// ########### END Read Coordinate System Origin ############
		}

		// ############################## END Read waypoints ###########################

		// NOTE:
		// The scene is loaded as the last step, since it takes the longest time.
		// In the case that something goes wrong during the parsing
		// of the survey description, the user is noticed immediately and does not need to wait
		// until the scene is loaded, only to learn that something has failed.

		// ########### BEGIN Load scene ############
		String sceneString = surveyNode.getAttribute("scene");

		Scene scene = (Scene) getAssetByLocation("scene", sceneString);

		if (scene == null) {
			System.out.println("Failed to load scene");
			System.exit(1);
		}

		survey.scanner.platform.scene = scene;

		// ########### END Load scene ############

		// ######## BEGIN Apply scene geometry shift to platform waypoint coordinates ###########
		for (Leg leg : survey.legs) {

			if (leg.mPlatformSettings != null) {
				leg.mPlatformSettings.setPosition(leg.mPlatformSettings.getPosition().subtract(survey.scanner.platform.scene.getShift()));

				// ############ BEGIN If specified, move waypoint z coordinate to ground level ###############
				if (leg.mPlatformSettings.onGround) {
					Vector3D pos = leg.mPlatformSettings.getPosition();

					Vector3D ground = scene.getGroundPointAt(pos);

					if (ground != null) {
						leg.mPlatformSettings.setPosition(new Vector3D(pos.getX(), pos.getY(), ground.getZ()));
					}
				}
				// ############ END If specified, move waypoint z coordinate to ground level ###############
			}
		}
		// ######## END Apply scene geometry shift to platform waypoint coordinates ###########

		return survey;
	}

	
	// TODO 3: Remove this method and get survey by id
	public Survey load() {

		NodeList surveyNodes = document.getElementsByTagName("survey");

		if (surveyNodes == null) {
			System.out.println("XML Survey playback loader: ERROR: No survey elements found in file " + this.xmlDocFilename);
			return null;
		}

		return createSurveyFromXml((Element) surveyNodes.item(0));
	}
}
