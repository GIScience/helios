package de.uni_hd.giscience.helios.assetsloading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import de.uni_hd.giscience.helios.core.scene.Scene;
import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;

// Class for loading and applying the material reflectances [0,100] from the ASTER Spectral Library
public class SpectralLibrary {
	
	private final String path = "assets/spectra";	
	private final double defaultReflectance = 50;
	private HashMap<String, Float> reflectanceMap;
	private float wavelength_um;						
	
	public SpectralLibrary(float wavelength_m) {	
		
		reflectanceMap = new HashMap<String, Float>();
		wavelength_um = wavelength_m * 1000000f;	// ASTER uses µm
	}
	
	private float interpolateReflectance(float w0, float w1, float r0, float r1) {
		
		float wRange = w1 - w0;
		float wShift = wavelength_um - w0;
		float factor = wShift / wRange;
		float rRange = r1 - r0;
	
		return r0 + (factor * rRange);
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
			
			for (int i = 0; i < 26; i++) {	// Skip the header
				line = br.readLine();
			}
			
			float wavelength = 0;
			float reflectance = 0;
			float prevWavelength = 0;
			float prevReflectance = 0;
			while ((line = br.readLine()) != null) {			
				String[] values = line.split("\t");
				wavelength = Float.parseFloat(values[0]);
				reflectance = Float.parseFloat(values[1]);
							
				if (wavelength < wavelength_um) {
					prevWavelength = wavelength;
					prevReflectance = reflectance;
					continue;
				}
				
				if (wavelength > wavelength_um) {
					reflectance = interpolateReflectance(prevWavelength, wavelength, prevReflectance, reflectance);
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
	
	public void readReflectances() {
		
		File folder = new File(path);
		System.out.print("Reading Spectral Library... ");
		
		if (!folder.exists()) {
			System.out.println("Error: folder " + path + " not found");
			return;
		}
		
		for (File file : folder.listFiles()) {		
			readFileAster(file);						
		}
		
		System.out.println(reflectanceMap.size() + " materials found");			
	}

	// TODO Jorge: Is more efficient to set the reflectances while reading the .obj
	public void setReflectances(Scene scene) {
		
		ArrayList<String> matsMissing = new ArrayList<>();
		
		for (Primitive prim : scene.primitives) {
			
			prim.material.reflectance = defaultReflectance;
			
			if (prim.material.spectra == null) {
				if (!matsMissing.contains(prim.material.spectra)) {
					matsMissing.add(prim.material.spectra);
					System.out.println("Warning: material " + prim.material.name + " (" + prim.material.matFilePath + ") has no spectral definition");
				}		
				continue;
			}
			
			if (!reflectanceMap.containsKey(prim.material.spectra)) {
				if (!matsMissing.contains(prim.material.spectra)) {
					matsMissing.add(prim.material.spectra);
					System.out.println("Warning: spectra " + prim.material.spectra + " (" + prim.material.matFilePath + ") is not in the spectral library");
				}
				continue;
			}
			
			prim.material.reflectance = reflectanceMap.get(prim.material.spectra);
			prim.material.setSpecularity();
		}
	}
}
