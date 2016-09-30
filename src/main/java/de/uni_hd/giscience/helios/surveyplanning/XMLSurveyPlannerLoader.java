package de.uni_hd.giscience.helios.surveyplanning;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.uni_hd.giscience.helios.assetsloading.XmlAssetsLoader;
import de.uni_hd.giscience.helios.core.platform.Platform;
import de.uni_hd.giscience.helios.core.scene.Scene;
import de.uni_hd.giscience.helios.core.scanner.Scanner;

public class XMLSurveyPlannerLoader extends XmlAssetsLoader {

	public XMLSurveyPlannerLoader(String filePath) {
		super(filePath);
	}

	public AbstractSurveyPlanner load() {

		AbstractSurveyPlanner result = null;

		// ################# BEGIN Read Position Finders ##################

		NodeList plannerNodes = document.getElementsByTagName("surveyPlanner");

		Element plannerNode = (Element) plannerNodes.item(0);

		String methodString = plannerNode.getAttribute("method").toLowerCase();

		if (methodString.equals("bestcoverage")) {
			result = new SurveyPlanner1();
		} else if (methodString.equals("reduce")) {
			result = new SurveyPlanner2();
		} else {
			// LidarSim.log.severe("Fatal error during survey planner loading: No planning method defined.");
			System.exit(-1);
		}

		result.id = plannerNode.getAttribute("id");

		result.scene = (Scene) getAssetByLocation("scene", plannerNode.getAttribute("scene"));
		System.out.println("blubb");

		result.platform = (Platform) getAssetByLocation("platform", plannerNode.getAttribute("platform"));
		result.scanner = (Scanner) getAssetByLocation("scanner", plannerNode.getAttribute("scanner"));

		result.cfg_minCoverage_percent = Double.parseDouble(plannerNode.getAttribute("minCoverage_percent"));
		result.cfg_hitTolerance_m = Double.parseDouble(plannerNode.getAttribute("hitTolerance_m"));
		result.cfg_interactive = Boolean.parseBoolean(plannerNode.getAttribute("interactive"));
		result.cfg_gridSizePSP_m = Double.parseDouble(plannerNode.getAttribute("gridSizePSP_m"));
		result.cfg_gridSizeVTP_m = Double.parseDouble(plannerNode.getAttribute("gridSizeVTP_m"));
		result.cfg_pspRemoveOrder = plannerNode.getAttribute("pspRemoveOrder");
		// result.outputPath = "surveyPlanning" + File.separator + result.id;

		// ################# END Read Position Finders ##################

		return result;
	}
}
