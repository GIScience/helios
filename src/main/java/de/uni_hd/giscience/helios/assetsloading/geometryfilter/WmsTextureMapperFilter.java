// TODO 4: Find a solution to define the wms image storage path that is independent of the input tif file!

package de.uni_hd.giscience.helios.assetsloading.geometryfilter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.apache.commons.validator.UrlValidator;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WMS1_1_0.GetMapRequest;
import org.geotools.data.wms.WMSUtils;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

import de.uni_hd.giscience.helios.assetsloading.ScenePart;
import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;

public class WmsTextureMapperFilter extends AbstractGeometryFilter {

	public Path resourcePath = null;
	public String wms_username = "";
	public String wms_password = "";
	public String dopLayer = "";

	public String wmsUrl = "";

	String requestEpsg = "EPSG:4326";

	public WmsTextureMapperFilter(ScenePart prims) {

		this.primsOut = prims;
	}

	public ScenePart run() {

		String modelFilePath = (String) params.get("filepath");
		resourcePath = FileSystems.getDefault().getPath(modelFilePath);

		// LidarSim.log.info("Applying WMS texture filter...");

		String imgFormat = "jpeg";

		String texFileName = resourcePath.getFileName() + "_orthophoto." + imgFormat;

		String fullFilePath = resourcePath.getParent().toString() + "/" + texFileName;

		File outputfile = new File(fullFilePath);

		for (Primitive p : primsOut.mPrimitives) {

			// Set texture file in material:
			p.material.matFilePath = resourcePath.getParent();
			p.material.map_Kd = texFileName;

			boolean forceReload = false;

			if (outputfile.isFile() && !forceReload) {
				// LidarSim.log.info("Orthophoto file already exists and reload is not forced. Skipping download.");
			} else {
				String username = (String) params.get("username");
				String password = (String) params.get("password");

				String urlstring = params.get("caps_url") + "?request=GetCapabilities&user=" + username + "&password=" + password;

				UrlValidator urlValidator = new UrlValidator();

				System.out.println(urlstring);

				if (!urlValidator.isValid(urlstring)) {
					return primsOut;
				}

				URL capabilitiesUrl = null;
				try {
					capabilitiesUrl = new URL(urlstring);
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}

				WebMapServer wms = null;
				ReferencedEnvelope requestEnvelope = null;
				try {
					ReferencedEnvelope refEnv = new ReferencedEnvelope(primsOut.mEnv);
					requestEnvelope = refEnv.transform(CRS.decode(requestEpsg), true);

					wms = new WebMapServer(capabilitiesUrl);

					WMSCapabilities capabilities = wms.getCapabilities();

					GetMapRequest request = (GetMapRequest) wms.createGetMapRequest();

					request.setFormat("image/" + imgFormat);
					request.setDimensions(1024, 1024); // sets the dimensions of the image to be returned from the server
					request.setSRS(requestEpsg);
					request.setBBox(requestEnvelope);

					int layersAdded = 0;
					for (Layer layer : WMSUtils.getNamedLayers(capabilities)) {
						if (layer.getName().equals(params.get("layer"))) {
							request.addLayer(layer);
							layersAdded++;
						}
					}

					if (layersAdded == 0) {
						System.out.println("ERROR: No layers added to request!");
						return primsOut;
					}

					request.setProperty("user", username);
					request.setProperty("password", password);

					System.out.println("WMS GetMap URL: " + request.getFinalURL());
					System.out.print("Fetching texture image from WMS... ");

					GetMapResponse response;
					BufferedImage image;

					response = (GetMapResponse) wms.issueRequest(request);
					System.out.println("success.");

					image = ImageIO.read(response.getInputStream());

					if (image != null) {
						System.out.println("Writing downloaded image to " + outputfile.getAbsolutePath());
						ImageIO.write(image, imgFormat, outputfile);
					} else {
						System.out.println("ERROR! Failed to download image from WMS.");
					}
				} catch (Exception e) {

					e.printStackTrace();
				}
			}
		}

		// LidarSim.log.info("WMS texture filter applied successfully.");

		return primsOut;
	}

}
