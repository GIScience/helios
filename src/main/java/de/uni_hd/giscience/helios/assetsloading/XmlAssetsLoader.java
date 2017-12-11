// TODO 4: Add central messages/logging method

package de.uni_hd.giscience.helios.assetsloading;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Color4f;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.uni_hd.giscience.helios.assetsloading.geometryfilter.AbstractGeometryFilter;
import de.uni_hd.giscience.helios.assetsloading.geometryfilter.AssumeMaterialsFilter;
import de.uni_hd.giscience.helios.assetsloading.geometryfilter.GeoTiffFileLoader;
import de.uni_hd.giscience.helios.assetsloading.geometryfilter.RotateFilter;
import de.uni_hd.giscience.helios.assetsloading.geometryfilter.ScaleFilter;
import de.uni_hd.giscience.helios.assetsloading.geometryfilter.TranslateFilter;
import de.uni_hd.giscience.helios.assetsloading.geometryfilter.WavefrontObjFileLoader;
import de.uni_hd.giscience.helios.assetsloading.geometryfilter.WmsTextureMapperFilter;
import de.uni_hd.giscience.helios.assetsloading.geometryfilter.XYZPointCloudFileLoader;
import de.uni_hd.giscience.helios.core.Asset;
import de.uni_hd.giscience.helios.core.platform.GroundVehiclePlatform;
import de.uni_hd.giscience.helios.core.platform.HelicopterPlatform;
import de.uni_hd.giscience.helios.core.platform.LinearPathPlatform;
import de.uni_hd.giscience.helios.core.platform.Platform;
import de.uni_hd.giscience.helios.core.platform.PlatformSettings;
import de.uni_hd.giscience.helios.core.platform.SimplePhysicsPlatform;
import de.uni_hd.giscience.helios.core.scanner.FWFSettings;
import de.uni_hd.giscience.helios.core.scanner.Scanner;
import de.uni_hd.giscience.helios.core.scanner.ScannerHead;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;
import de.uni_hd.giscience.helios.core.scanner.beamDeflector.AbstractBeamDeflector;
import de.uni_hd.giscience.helios.core.scanner.beamDeflector.ConicBeamDeflector;
import de.uni_hd.giscience.helios.core.scanner.beamDeflector.FiberArrayBeamDeflector;
import de.uni_hd.giscience.helios.core.scanner.beamDeflector.OscillatingMirrorBeamDeflector;
import de.uni_hd.giscience.helios.core.scanner.beamDeflector.PolygonMirrorBeamDeflector;
//import de.uni_hd.giscience.helios.core.scanner.detector.SingleRayPulseDetector;
import de.uni_hd.giscience.helios.core.scanner.detector.FullWaveformPulseDetector;
import de.uni_hd.giscience.helios.core.scanner.detector.SingleRayPulseDetector;
import de.uni_hd.giscience.helios.core.scene.Scene;
import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;
import de.uni_hd.giscience.helios.core.scene.primitives.Vertex;

public class XmlAssetsLoader {

	protected org.w3c.dom.Document document = null;

	// protected Logger log;

	protected String xmlDocFilename = "unknown.xml";
	protected String xmlDocFilePath = "";

	public XmlAssetsLoader(String filePath) {

		File xmlFile = new File(filePath);

		xmlDocFilename = xmlFile.getName();
		xmlDocFilePath = xmlFile.getPath();

		// ################# BEGIN Load XML Document ###################
		/*
		 * DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		 * 
		 * DocumentBuilder dBuilder = null; try { dBuilder = dbFactory.newDocumentBuilder(); } catch (ParserConfigurationException e) { e.printStackTrace(); }
		 */
		try {
			// Built-in XML document parser:
			// document = dBuilder.parse(xmlFile);

			// Parse document with custom PositionalXmlReader to get the line numbers of each element:
			// Source: http://www.javavillage.in/view-topic.php?tag=get-line-numbers-from-xml-node-java
			// TODO 4: Copyright?
			InputStream is = new FileInputStream(xmlFile);
			document = PositionalXMLReader.readXML(is);
			is.close();

		} catch (SAXException | IOException e) {
			System.out.println("ERROR: Failed to open or parse XML file: " + filePath);
			System.exit(-1);
			// e.printStackTrace();
			return;
		}

		document.getDocumentElement().normalize();
		// ################# END Load XML Document ###################
	}

	// ################# END get(asset) by id methods #############

	// ################# BEGIN (asset)FromXML methods #############
	public Asset createAssetFromXml(String type, Element assetNode) {

		if (assetNode == null) {
			System.out.println("ERROR: Asset definition XML node is null!");
			System.exit(-1);
		}

		Asset result = null;

		// TODO 4: Unflexible. Try to build class name from type string.

		if (type == "platform") {
			result = (Asset) createPlatformFromXml(assetNode);
		} else if (type == "platformSettings") {
			result = (Asset) createPlatformSettingsFromXml(assetNode);
		} else if (type == "scanner") {
			result = (Asset) createScannerFromXml(assetNode);
		} else if (type == "scene") {
			result = (Asset) createSceneFromXml(assetNode, xmlDocFilePath);
		} else if (type == "scannerSettings") {
			result = (Asset) createScannerSettingsFromXml(assetNode);
		} else if (type == "FWFSettings") {
			result = (Asset) createFWFSettingsFromXml(assetNode);
		} else {
			System.out.println("ERROR: Unknown asset type: " + type);
			System.exit(-1);
		}

		// Read "asset" properties:
		result.id = (String) getAttribute(assetNode, "id", String.class, null);
		result.name = (String) getAttribute(assetNode, "name", String.class, "Unnamed " + type + " asset");

		// Store source file path for possible later XML export:
		result.sourceFilePath = xmlDocFilePath;

		return result;
	}
	// ################# END get(asset) by id methods #############

	public Color4f createColorFromXml(Element node) {

		try {
			float r = Float.parseFloat(node.getAttribute("r"));
			float g = Float.parseFloat(node.getAttribute("g"));
			float b = Float.parseFloat(node.getAttribute("b"));

			Color4f col = new Color4f(r, g, b, 1);

			return col;
		} catch (Exception e) {
			return null;
		}
	}

	public HashMap<String, Object> createParamsFromXml(Element paramsNode) {

		HashMap<String, Object> result = new HashMap<>();

		if (paramsNode == null) {
			return null;
		}

		NodeList paramNodes = paramsNode.getElementsByTagName("param");

		if (paramNodes == null) {
			return null;
		}

		for (int ii = 0; ii < paramNodes.getLength(); ii++) {

			try {
				Element pnode = (Element) paramNodes.item(ii);

				String type = pnode.getAttribute("type");
				String key = pnode.getAttribute("key");
				String valueString = pnode.getAttribute("value");

				if (type == null || type.equals("") || type.equals("string")) {
					result.put(key, valueString);
				} else {

					if (type.equals("boolean")) {
						result.put(key, Boolean.parseBoolean(valueString));
					} else if (type.equals("double")) {
						result.put(key, Double.parseDouble(valueString));
					} else if (type.equals("rotation")) {
						result.put(key, createRotationFromXml(pnode));
					} else if (type.equals("vec3")) {

						String[] coords = valueString.split(";");

						double x = Double.parseDouble(coords[0]);
						double y = Double.parseDouble(coords[1]);
						double z = Double.parseDouble(coords[2]);

						result.put(key, new Vector3D(x, y, z));
					}
				}

			} catch (Exception e) {
				// log.warning("Failed to read filter parameter: " + e.getMessage());
			}
		}

		return result;
	}

	public Platform createPlatformFromXml(Element platformNode) {

		Platform platform = new Platform();

		// Read platform type:
		String type = platformNode.getAttribute("type").toLowerCase();

		if (type.equals("groundvehicle")) {
			platform = new GroundVehiclePlatform();
		} else if (type.equals("linearpath")) {
			platform = new LinearPathPlatform();
		} else if (type.equals("multicopter")) {
			platform = new HelicopterPlatform();
		} else {
			System.out.println("No platform type specified. Using dummy platform.");
		}

		// ############# BEGIN Read SimplePhysicsPlatform-related stuff ###############
		if (SimplePhysicsPlatform.class.isAssignableFrom(platform.getClass())) {

			SimplePhysicsPlatform spp = (SimplePhysicsPlatform) platform;

			// spp.cfg_mass_kg = (double) getAttribute(platformNode, "mass_kg", Double.class, null);
			spp.mCfg_drag = (double) getAttribute(platformNode, "drag", Double.class, null);
		}
		// ############# END Read SimplePhysicsPlatform-related stuff ###############

		// Read 3D model for visualization:
		platform.cfg_device_visModelPath = platformNode.getAttribute("model");

		// Read relative scanner rotation:
		try {
			Element scannerMountNode = (Element) platformNode.getElementsByTagName("scannerMount").item(0);

			// Read relative position of the scanner mount on the platform:
			platform.cfg_device_relativeMountPosition = createVec3dFromXml(scannerMountNode, "");

			// Read relative orientation of the scanner mount on the platform:
			platform.cfg_device_relativeMountAttitude = createRotationFromXml(scannerMountNode);
		} catch (Exception e) {
			// log.warning("No scanner orientation defined.");
		}

		return platform;
	}

	public PlatformSettings createPlatformSettingsFromXml(Element node) {

		PlatformSettings settings = new PlatformSettings();

		PlatformSettings template = new PlatformSettings();

		if (node.hasAttribute("template")) {
			PlatformSettings bla = (PlatformSettings) getAssetByLocation("platformSettings", node.getAttribute("template"));

			if (bla != null) {
				template = bla;

				// ATTENTION:
				// We need to temporarily convert the head rotation settings from radians back to degrees, since degrees
				// is the unit in which they are read from the XML, and below, the template settings are used as defaults
				// in case that a value is not specified in the XML!

			} else {
				System.out.println("XML Assets Loader: WARNING: Platform settings template specified in line " + node.getUserData("lineNumber") + " not found: '" + template
						+ "'. Using hard-coded defaults instead.");
			}
		}

		// Read platform coordinates
		settings.x = (Double) getAttribute(node, "x", Double.class, template.x);
		settings.y = (Double) getAttribute(node, "y", Double.class, template.y);
		settings.z = (Double) getAttribute(node, "z", Double.class, template.z);

		// Read if scanner should be put on ground, ignoring z coordinate:
		settings.onGround = (Boolean) getAttribute(node, "onGround", Boolean.class, template.onGround);

		// Read platform speed:
		settings.movePerSec_m = (Double) getAttribute(node, "movePerSec_m", Double.class, template.movePerSec_m);

		return settings;
	}

	public Scanner createScannerFromXml(Element scannerNode) {

		// ############ BEGIN Read emitter position and orientation ############
		Vector3D emitterPosition = null;
		Rotation emitterAttitude = null;

		try {
			Element emitterNode = (Element) scannerNode.getElementsByTagName("beamOrigin").item(0);

			// Read relative position of the scanner mount on the platform:
			emitterPosition = createVec3dFromXml(emitterNode, "");

			// Read relative orientation of the scanner mount on the platform:
			emitterAttitude = createRotationFromXml(emitterNode);
		} catch (Exception e) {
			// log.warning("No scanner orientation defined.");
		}

		if (emitterPosition == null) {
			emitterPosition = new Vector3D(0, 0, 0);
		}

		if (emitterAttitude == null) {
			emitterAttitude = new Rotation(new Vector3D(1, 0, 0), 0);
		}
		// ############ END Read emitter position and orientation ############

		// ########## BEGIN Read supported pulse frequencies ############
		String pulseFreqsString = (String) getAttribute(scannerNode, "pulseFreqs_Hz", String.class, "");

		ArrayList<Integer> pulseFreqs = new ArrayList<Integer>();

		String[] freqs = pulseFreqsString.split(",");

		for (String freq : freqs) {
			pulseFreqs.add(Integer.parseInt(freq));
		}
		// ########## END Read supported pulse frequencies ############

		// ########### BEGIN Read all the rest #############
		Double beamDiv_rad = (Double) getAttribute(scannerNode, "beamDivergence_rad", Double.class, 0.0003d);

		Double pulseLength_ns = (Double) getAttribute(scannerNode, "pulseLength_ns", Double.class, 1.0d);

		String visModel = (String) getAttribute(scannerNode, "visModel", String.class, "");
		
		String id = (String) getAttribute(scannerNode, "id", String.class, "Default");
		
		Double avgPower = (Double) getAttribute(scannerNode, "averagePower_w", Double.class, 4.0);
		
		Double beamQuality = (Double) getAttribute(scannerNode, "beamQualityFactor", Double.class, 1.0);
		
		Double efficiency = (Double) getAttribute(scannerNode, "opticalEfficiency", Double.class, 0.99);

		Double receiverDiameter = (Double) getAttribute(scannerNode, "receiverDiameter_m", Double.class, 0.15);

		Double visibility = (Double) getAttribute(scannerNode, "atmosphericVisibility_km", Double.class, 23d);
		
		Integer wavelength = (Integer) getAttribute(scannerNode, "wavelength_nm", Integer.class, 1064);
		// ########### END Read all the rest #############

		Scanner scanner = new Scanner(beamDiv_rad, emitterPosition, emitterAttitude, pulseFreqs, pulseLength_ns, visModel, id, avgPower, beamQuality, efficiency, receiverDiameter, visibility, wavelength);

		// ############################# BEGIN Configure scanner head ##############################
		// ################### BEGIN Read Scan head rotation axis #############
		Vector3D headRotateAxis = new Vector3D(0, 0, 1);

		try {
			Vector3D axis = createVec3dFromXml((Element) scannerNode.getElementsByTagName("headRotateAxis").item(0), "");

			if (axis != null && axis.getNorm() > 0.1) {
				headRotateAxis = axis;
			}
		} catch (Exception e) {
			// TODO 4: Implement central method for error messages
			System.out.println(
					"XML Assets Loader: Failed to read child element <headRotateAxis> of <scanner> element at line " + scannerNode.getUserData("lineNumber") + ". Using default.");
		}
		// ############### END Read Scan head rotation axis ###############

		// Read head rotation speed:
		Double headRotatePerSecMax_rad = (Double) getAttribute(scannerNode, "headRotatePerSecMax_deg", Double.class, 0.0d) * (Math.PI / 180);

		// Configure scanner head:
		scanner.scannerHead = new ScannerHead(headRotateAxis, headRotatePerSecMax_rad);

		// ############################# END Configure scanner head ##############################

		// ################################## BEGIN Configure beam deflector ######################################

		// ########### BEGIN Read and apply generic properties ##########
		Double scanFreqMax_Hz = (Double) getAttribute(scannerNode, "scanFreqMax_Hz", Double.class, null);
		Double scanFreqMin_Hz = (Double) getAttribute(scannerNode, "scanFreqMin_Hz", Double.class, null);
		Double scanAngleMax_rad = (Double) getAttribute(scannerNode, "scanAngleMax_deg", Double.class, 0) * (Math.PI / 180);
		// ########### END Read and apply generic properties ##########

		String str_opticsType = scannerNode.getAttribute("optics");

		AbstractBeamDeflector beamDeflector = null;

		if (str_opticsType.equals("oscillating")) {

			int scanProduct = (Integer) getAttribute(scannerNode, "scanProduct", Integer.class, 1000000);
			beamDeflector = new OscillatingMirrorBeamDeflector(scanAngleMax_rad, scanFreqMax_Hz, scanFreqMin_Hz, scanProduct);
		} else if (str_opticsType.equals("conic")) {
			beamDeflector = new ConicBeamDeflector(scanAngleMax_rad, scanFreqMax_Hz, scanFreqMin_Hz);
		} else if (str_opticsType.equals("line")) {
			int numFibers = (Integer) getAttribute(scannerNode, "numFibers", Integer.class, 1);
			beamDeflector = new FiberArrayBeamDeflector(scanAngleMax_rad, scanFreqMax_Hz, scanFreqMin_Hz, numFibers);
		} else if (str_opticsType.equals("rotating")) {

			Double scanAngleEffectiveMax_rad = (Double) getAttribute(scannerNode, "scanAngleEffectiveMax_deg", Double.class, 0) * (Math.PI / 180);
			beamDeflector = new PolygonMirrorBeamDeflector(scanFreqMax_Hz, scanFreqMin_Hz, scanAngleMax_rad, scanAngleEffectiveMax_rad);
		}

		if (beamDeflector == null) {
			System.out.println("ERROR: Unknown beam deflector type: '" + str_opticsType + "'. Aborting.");
			System.exit(1);
		}

		scanner.beamDeflector = beamDeflector;

		// ################################## END Configure beam deflector #######################################

		// ############################ BEGIN Configure detector ###############################
		Double rangeMin_m = (Double) getAttribute(scannerNode, "rangeMin_m", Double.class, 0.0d);
		Double accuracy_m = (Double) getAttribute(scannerNode, "accuracy_m", Double.class, 0.0d);
		scanner.detector = new FullWaveformPulseDetector(scanner, accuracy_m, rangeMin_m);
	//	scanner.detector = new SingleRayPulseDetector(scanner, accuracy_m, rangeMin_m);
		// ############################ END Configure detector ###############################

		return scanner;
	}

	public ScannerSettings createScannerSettingsFromXml(Element node) {
		ScannerSettings settings = new ScannerSettings();

		ScannerSettings template = new ScannerSettings();

		template.active = true;
		template.headRotatePerSec_rad = 0d;
		template.headRotateStart_rad = 0d;
		template.headRotateStop_rad = 0d;
		template.pulseFreq_Hz = null;
		template.scanAngle_rad = 0;
		template.scanFreq_Hz = null;

		if (node.hasAttribute("template")) {
			ScannerSettings bla = (ScannerSettings) getAssetByLocation("scannerSettings", node.getAttribute("template"));

			if (bla != null) {
				template = bla;

				// ATTENTION:
				// We need to temporarily convert the head rotation settings from radians back to degrees, since degrees
				// is the unit in which they are read from the XML, and below, the template settings are used as defaults
				// in case that a value is not specified in the XML!
				template.headRotatePerSec_rad *= (180.0 / Math.PI);
				template.headRotateStart_rad *= (180.0 / Math.PI);
				template.headRotateStop_rad *= (180.0 / Math.PI);
				template.scanAngle_rad *= (180.0 / Math.PI);
			} else {
				System.out.println("XML Assets Loader: WARNING: Scanner settings template specified in line " + node.getUserData("lineNumber") + " not found: '" + template
						+ "'. Using hard-coded defaults instead.");
			}
		}

		settings.active = (Boolean) getAttribute(node, "active", Boolean.class, template.active);
		settings.beamSampleQuality = (Integer) getAttribute(node, "beamSampleQuality", Integer.class, template.beamSampleQuality);
		settings.headRotatePerSec_rad = (Double) getAttribute(node, "headRotatePerSec_deg", Double.class, template.headRotatePerSec_rad) * (Math.PI / 180);
		settings.headRotateStart_rad = (Double) getAttribute(node, "headRotateStart_deg", Double.class, template.headRotateStart_rad) * (Math.PI / 180);

		double hrStop_rad = (Double) getAttribute(node, "headRotateStop_deg", Double.class, template.headRotateStop_rad) * (Math.PI / 180);

		// Make sure that rotation stop angle is larger than rotation start angle if rotation speed is positive:
		if (hrStop_rad < settings.headRotateStart_rad && settings.headRotatePerSec_rad > 0) {
			System.out.println("XML Assets Loader: Error: Head Rotation Stop angle must be larger than start angle if rotation speed is positive!");
			System.exit(-1);
		}

		// Make sure that rotation stop angle is larger than rotation start angle if rotation speed is positive:
		if (hrStop_rad > settings.headRotateStart_rad && settings.headRotatePerSec_rad < 0) {
			System.out.println("XML Assets Loader: Error: Head Rotation Stop angle must be smaller than start angle if rotation speed is negative!");
			System.exit(-1);
		}

		settings.headRotateStop_rad = hrStop_rad;

		settings.pulseFreq_Hz = (Integer) getAttribute(node, "pulseFreq_hz", Integer.class, template.pulseFreq_Hz);
		settings.scanAngle_rad = (Double) getAttribute(node, "scanAngle_deg", Double.class, template.scanAngle_rad) * (Math.PI / 180);
		settings.scanFreq_Hz = (Integer) getAttribute(node, "scanFreq_hz", Integer.class, template.scanFreq_Hz);

		return settings;
	}
	

	public FWFSettings createFWFSettingsFromXml(Element node) {
		FWFSettings settings = new FWFSettings();

		settings.numTimeBins = (Integer) getAttribute(node, "numTimeBins", Integer.class, settings.numTimeBins);
		settings.numFullwaveBins = (Integer) getAttribute(node, "numFullwaveBins", Integer.class, settings.numFullwaveBins);
		settings.minEchoWidth = (Double) getAttribute(node, "minEchoWidth", Double.class, settings.minEchoWidth);
		settings.peakEnergy = (Double) getAttribute(node, "peakEnergy", Double.class, settings.peakEnergy);
		settings.apartureDiameter = (Double) getAttribute(node, "apartureDiameter", Double.class, settings.apartureDiameter);
		settings.scannerEfficiency = (Double) getAttribute(node, "scannerEfficiency", Double.class, settings.scannerEfficiency);
		settings.atmosphericVisibility = (Double) getAttribute(node, "atmosphericVisibility", Double.class, settings.atmosphericVisibility);
		settings.scannerWaveLength = (Double) getAttribute(node, "scannerWaveLength", Double.class, settings.scannerWaveLength);
		settings.beamDivergence_rad = (Double) getAttribute(node, "beamDivergence_rad", Double.class, settings.beamDivergence_rad);
		settings.pulseLength_ns = (Double) getAttribute(node, "pulseLength_ns", Double.class, settings.pulseLength_ns);
		settings.beamSampleQuality = (Integer) getAttribute(node, "beamSampleQuality", Integer.class, settings.beamSampleQuality);
		settings.winSize = (Integer) getAttribute(node, "winSize", Integer.class, settings.winSize);

		return settings;
	}

	// ################# END get(asset) by id methods #############

	public Scene createSceneFromXml(Element sceneNode, String path) {

		Scene scene = new Scene();
		scene.sourceFilePath = path;

		// ############## BEGIN Read sun direction ###############
		try {
			Element sunNode = (Element) sceneNode.getElementsByTagName("sunDir").item(0);

			if (sunNode != null) {
				scene.sunDir = createVec3dFromXml(sunNode, "");
			}
		} catch (Exception e) {
		}
		// ############## END Read sun direction ###############

		// ############## BEGIN Sky color ###############
		try {
			Element skyNode = (Element) sceneNode.getElementsByTagName("sky").item(0);

			if (skyNode != null) {
				scene.skyColor = createColorFromXml(skyNode);
			}
		} catch (Exception e) {
		}
		// ############## END Read sky color ###############

		// ############# BEGIN Read skybox textures and azimuth ##################
		try {
			Element skyboxNode = (Element) sceneNode.getElementsByTagName("skybox").item(0);

			if (skyboxNode != null) {

				scene.skyboxAzimuth_rad = (float) ((float) getAttribute(skyboxNode, "azimuth_deg", Float.class, 0f) * (Math.PI / 180));
				scene.skyboxTexturesFolder = (String) getAttribute(skyboxNode, "texturesFolder", String.class, "");

				System.out.println("Skybox folder: " + scene.skyboxTexturesFolder);
			}
		} catch (Exception e) {
			System.out.println("Failed to read skybox element.");

		}
		// ############# END Read skybox textures and azimuth ##################

		// ####################### BEGIN Loop over all part nodes ############################

		NodeList scenePartNodes = sceneNode.getElementsByTagName("part");

		for (int partIndex = 0; partIndex < scenePartNodes.getLength(); partIndex++) {

			ScenePart scenePart = new ScenePart();

			Element partNode = (Element) scenePartNodes.item(partIndex);

			// ############## BEGIN Loop over filter nodes ##################
			NodeList filterNodes = partNode.getElementsByTagName("filter");

			for (int kk = 0; kk < filterNodes.getLength(); kk++) {

				// TODO 5: Reimplement CRS transform filter

				Element filterNode = (Element) filterNodes.item(kk);

				String filterType = filterNode.getAttribute("type").toLowerCase();

				AbstractGeometryFilter filter = null;

				// ################### BEGIN Set up filter ##################

				// Apply assume materials filter:
				if (filterType.equals("assumematerials")) {
					filter = new AssumeMaterialsFilter(scenePart, scene.sunDir);
				}

				// Apply scale transformation:
				else if (filterType.equals("scale")) {
					filter = new ScaleFilter(scenePart);
				}

				// Read GeoTiff file:
				else if (filterType.equals("geotiffloader")) {
					filter = new GeoTiffFileLoader();
				}

				// Read Wavefront Object file:
				else if (filterType.equals("objloader")) {
					filter = new WavefrontObjFileLoader();
				}

				// Apply rotation filter:
				else if (filterType.equals("rotate")) {
					filter = new RotateFilter(scenePart);
				}

				// Apply translate transformation:
				else if (filterType.equals("translate")) {
					filter = new TranslateFilter(scenePart);
				}

				// Apply WMS Orthophoto filter:
				else if (filterType.equals("wms")) {

					filter = new WmsTextureMapperFilter(scenePart);
				}

				// Read xyz ASCII point cloud file:
				else if (filterType.equals("xyzloader")) {

					// Read defined point cloud color:
					Color4f color = createColorFromXml((Element) partNode.getElementsByTagName("color").item(0));

					filter = new XYZPointCloudFileLoader(color);
				}

				// ################### END Set up filter ##################

				// Finally, apply the filter:
				if (filter != null) {

					// Set params:
					filter.params = createParamsFromXml(filterNode);

					System.out.println("Applying filter: " + filterType);
					scenePart = filter.run();
				}
			}
			// ############## END Loop over filter nodes ##################

			// ######### BEGIN Read and set scene part ID #########
			String partId = partNode.getAttribute("id");

			/*
			 * if (partId.equals("")) { System.out.println("ERROR: No part ID specified!"); System.exit(-1); }
			 */

			if (partId.equals("")) {
				scenePart.mId = String.valueOf(partIndex);
			} else {
				scenePart.mId = partId;
			}

			// ######### END Read and set scene part ID #########

			// For all primitives, set reference to their scene part:
			for (Primitive p : scenePart.mPrimitives) {
				p.part = scenePart;
			}

			
			// TODO 3: Don't apply the transformations in-place
			for (Vertex v : scenePart.getAllVertices()) {

				// Apply rotation:
				v.pos = scenePart.mRotation.applyTo(v.pos);

				if (v.normal != null) {
					v.normal = scenePart.mRotation.applyTo(v.normal);
				}

				// Apply scale:
				v.pos = new Vector3D(v.pos.getX() * scenePart.mScale, v.pos.getY() * scenePart.mScale, v.pos.getZ() * scenePart.mScale);

				// Apply translation:
				v.pos = v.pos.add(scenePart.mOrigin);
			}

			// Add scene part to the scene:
			scene.primitives.addAll(scenePart.mPrimitives);
		}
		// ####################### END Loop over all part nodes ############################

		boolean success = scene.finalizeLoading();

		if (!success) {
			// log.severe("Finalizing the scene failed.");
			System.exit(-1);
		}

		return scene;
	}

	public Rotation createRotationFromXml(Element rotGroupNode) {

		Rotation r = new Rotation(new Vector3D(1, 0, 0), 0);
		Rotation r2 = new Rotation(new Vector3D(1, 0, 0), 0);

		if (rotGroupNode == null) {
			return r;
		}

		NodeList rotNodes = rotGroupNode.getElementsByTagName("rot");

		for (int ii = 0; ii < rotNodes.getLength(); ii++) {

			Element rotNode = (Element) rotNodes.item(ii);

			String axis = rotNode.getAttribute("axis");

			double angle_rad = Double.parseDouble(rotNode.getAttribute("angle_deg")) * (Math.PI / 180);

			if (angle_rad != 0) {
				switch (axis) {
				case "yaw":
					r2 = new Rotation(new Vector3D(0, 0, 1), angle_rad);
					break;
				case "pitch":
					r2 = new Rotation(new Vector3D(1, 0, 0), angle_rad);
					break;
				case "roll":
					r2 = new Rotation(new Vector3D(0, 1, 0), angle_rad);
					break;
				}

				r = r2.applyTo(r);
			}
		}

		return r;
	}

	public Vector3D createVec3dFromXml(Element node, String attrPrefix) {

		if (node == null) {
			return null;
		}

		Double x = (Double) getAttribute(node, attrPrefix + "x", Double.class, 0.0);
		Double y = (Double) getAttribute(node, attrPrefix + "y", Double.class, 0.0);
		Double z = (Double) getAttribute(node, attrPrefix + "z", Double.class, 0.0);

		return new Vector3D(x, y, z);
	}

	public Asset getAssetById(String type, String id) {

		//try {
			NodeList assetNodes = document.getElementsByTagName(type);

			for (int ii = 0; ii < assetNodes.getLength(); ii++) {

				Element node = (Element) assetNodes.item(ii);

				if (node.getAttribute("id").equals(id)) {
					return createAssetFromXml(type, node);
				}
			}

			System.out.println("ERROR: " + type + " asset definition not found: " + this.xmlDocFilePath + "#" + id);
/*
		} catch (Exception e) {
			System.out.println("ERROR: Failed to read " + type + " asset definition: " + this.xmlDocFilePath + "#" + id);
		}
*/
		return null;
	}

	public Asset getAssetByLocation(String type, String location) {

		String[] bla = location.split("#");
		
		XmlAssetsLoader loader = this;
		String id = bla[0].trim();

		// External document location provided:
		if (bla.length == 2) {
			loader = new XmlAssetsLoader(bla[0].trim());
			id = bla[1].trim();
		}

		return loader.getAssetById(type, id);
	}

	// ################# BEGIN (small stuff)FromXML methods #############

	protected Object getAttribute(Element element, String attrName, Class<?> type, Object defaultVal) {

		Object result = null;

		try {
			if (!element.hasAttribute(attrName)) {
				throw new Exception("Attibute '" + attrName + "' does not exist!");
			}

			String attrVal = element.getAttribute(attrName);

			if (type == Boolean.class) {
				result = Boolean.parseBoolean(attrVal);
			} else if (type == Double.class) {
				result = Double.parseDouble(attrVal);
			} else if (type == Float.class) {
				result = Float.parseFloat(attrVal);
			} else if (type == Integer.class) {
				result = Integer.parseInt(attrVal);
			} else if (type == String.class) {
				result = attrVal;
			}
		}

		catch (Exception e) {

			// System.out.print("XML Assets Loader: Failed to read attribute '" + attrName + "' of <" + element.getNodeName() + "> element in line "
			// + element.getUserData("lineNumber") + ". ");

			if (defaultVal != null) {
				result = defaultVal;
				// System.out.println("Using default value: '" + defaultVal.toString() + "'.");
			} else {
				System.out.println("ERROR: No default value specified for attribute '" + attrName + "'. Aborting.");
				System.exit(-1);
			}
		}

		return result;
	}
}
