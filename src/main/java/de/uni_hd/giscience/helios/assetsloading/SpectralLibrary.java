package de.uni_hd.giscience.helios.assetsloading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import de.uni_hd.giscience.helios.core.scene.Scene;
import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;

// Class for loading and applying the material reflectances from the ASTER Spectral Library
public class SpectralLibrary {
	
	public final String path = "assets/spectrum";	
	public final double defaultReflectance = 100/2;
	public HashMap<String, Float> reflectanceMap;
	public double wavelength_um;						
	
	public SpectralLibrary(float wavelength_m) {	
		
		reflectanceMap = new HashMap<String, Float>();
		wavelength_um = wavelength_m * 1000000f;	// ASTER uses µm
	}
	
	private void readFileAster(File file) {	
		
		String fileName = file.getName();
		if (fileName.indexOf(".") > 0) {
			fileName = fileName.substring(0, fileName.lastIndexOf("."));
		}	
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr); 
			String line = null;				 
			
			for( int i = 0; i < 26; i++) {	// Skip the header
				line = br.readLine();
			}
			
			float wavelength = 0;
			float reflectance = 0;
			float prevReflectance = 0;
			while ((line = br.readLine()) != null) {			
				String[] values = line.split("\t");
				wavelength = Float.parseFloat(values[0]);
				reflectance = Float.parseFloat(values[1]);
							
				if (wavelength < wavelength_um) {
					prevReflectance = reflectance;
					continue;
				}
				
				if (wavelength > wavelength_um) {
					reflectance = (reflectance + prevReflectance) / 2;  // TODO: Do proper calculation; handle boundaries
				}
				
				break;
			}
			
			reflectanceMap.put(fileName, reflectance);
			
			br.close();
			fr.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadReflectances() {
		
		File folder = new File(path);
		System.out.println("Reading Spectral Library...");
		
		if(!folder.exists()) {
			System.out.println("Error: folder " + path + " not found");
			return;
		}
		
		for (File file : folder.listFiles()) {		
			readFileAster(file);						
		}
		
		System.out.println(reflectanceMap.toString());			
	}

	public void setReflectances(Scene scene) {
		
		for (Primitive prim : scene.primitives) {
			
			if(prim.material.definition == null) {
				//System.out.println("Warning: material " + prim.material.name + " has no spectral definition");
				prim.material.reflectance = defaultReflectance;
				continue;
			}
			
			if(!reflectanceMap.containsKey(prim.material.definition)) {
				System.out.println("Warning: material " + prim.material.definition +  " is not in the spectral library");
				continue;
			}
			
			prim.material.reflectance = reflectanceMap.get(prim.material.definition);
			prim.material.setSpecularity();
		}
	}
}
