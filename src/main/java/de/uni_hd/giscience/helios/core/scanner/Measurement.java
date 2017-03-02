package de.uni_hd.giscience.helios.core.scanner;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Measurement {

	public String hitObjectId = null;

	public Vector3D position = new Vector3D(0, 0, 0);
	public Vector3D beamDirection = new Vector3D(0, 0, 0);
	public Vector3D beamOrigin = new Vector3D(0, 0, 0);
	public double distance = 0;
	public double intensity = 0;
	public int returnNumber;
	public int fullwaveIndex;
	public Long gpsTime;
}
