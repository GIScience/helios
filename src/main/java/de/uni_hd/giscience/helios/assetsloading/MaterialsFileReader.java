package de.uni_hd.giscience.helios.assetsloading;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;

import de.uni_hd.giscience.helios.core.scene.Material;

public class MaterialsFileReader {

	public static HashMap<String, Material> loadMaterials(String filePathString) {

		Path matFilePath = FileSystems.getDefault().getPath(filePathString);

		HashMap<String, Material> newMats = new HashMap<>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePathString));
		} catch (FileNotFoundException e) {
			System.out.println("Failed to load materials file: " + filePathString);
			return newMats;
		}

		System.out.println("Reading materials from .mtl file '" + filePathString);

		try {

			Material newMat = null;

			String line;

			while ((line = br.readLine()) != null) {

				String[] lineParts = line.split("\\s+");

				// ####### BEGIN Wavefront .mtl standard attributes #########
				if (lineParts[0].equals("newmtl") && lineParts.length >= 2) {

					// Before starting a new material, put previous material to the map:
					if (newMat != null) {
						newMats.put(newMat.name, newMat);
					}

					newMat = new Material();
					newMat.matFilePath = matFilePath.getParent();
					newMat.name = lineParts[1];

				} else if (lineParts[0].equals("Ka") && lineParts.length >= 4) {
					newMat.ka[0] = Float.parseFloat(lineParts[1]);
					newMat.ka[1] = Float.parseFloat(lineParts[2]);
					newMat.ka[2] = Float.parseFloat(lineParts[3]);
				} else if (lineParts[0].equals("Kd") && lineParts.length >= 4) {
					newMat.kd[0] = Float.parseFloat(lineParts[1]);
					newMat.kd[1] = Float.parseFloat(lineParts[2]);
					newMat.kd[2] = Float.parseFloat(lineParts[3]);
				} else if (lineParts[0].equals("Ks") && lineParts.length >= 4) {
					newMat.ks[0] = Float.parseFloat(lineParts[1]);
					newMat.ks[1] = Float.parseFloat(lineParts[2]);
					newMat.ks[2] = Float.parseFloat(lineParts[3]);

				} else if (lineParts[0].equals("map_Kd") && lineParts.length >= 2) {
					newMat.map_Kd = lineParts[1];
				}
				// ####### END Wavefront .mtl standard attributes #########

				// ######### BEGIN HELIOS-specific additions to the wavefront .mtl standard #########
				else if (lineParts[0].equals("helios_reflectance") && lineParts.length >= 2) {
					newMat.reflectance = Double.parseDouble(lineParts[1]);
				}

				else if (lineParts[0].equals("helios_isGround") && lineParts.length >= 2) {
					newMat.isGround = Boolean.parseBoolean(lineParts[1]);
				}

				else if (lineParts[0].equals("helios_useVertexColors") && lineParts.length >= 2) {
					newMat.useVertexColors = Boolean.parseBoolean(lineParts[1]);
				}

				else if (lineParts[0].equals("helios_castShadows") && lineParts.length >= 2) {
					newMat.castShadows = Integer.parseInt(lineParts[1]);
				}

				else if (lineParts[0].equals("helios_receiveShadows") && lineParts.length >= 2) {
					newMat.receiveShadows = Integer.parseInt(lineParts[1]);
				}
				// ######### END HELIOS-specific additions to the wavefront .mtl standard #########
			}

			// Don't forget to put final material to the map:
			newMats.put(newMat.name, newMat);

			// LidarSim.log.info(newMats.size() + " material(s) loaded.");

		} catch (IOException e) {

			// LidarSim.log.warning("Failed to load materials file: " + filePathString);
		}

		finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return newMats;
	}
}
