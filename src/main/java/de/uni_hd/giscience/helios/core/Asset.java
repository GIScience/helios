package de.uni_hd.giscience.helios.core;

public class Asset {

	public String id = "";
	public String name = "Unnamed Asset";
	public String sourceFilePath = "";

	
	public String getLocationString() {
		return sourceFilePath + "#" + id;
	}
}
