package de.uni_hd.giscience.helios;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/*
 * Coordinate system convention for the lidar sim:
 * 
 * x - right
 * y - forward
 * z - up
 */

public class Directions {
	public static final Vector3D forward = new Vector3D(0, 1, 0);
	public static final Vector3D right = new Vector3D(1, 0, 0);
	public static final Vector3D up = new Vector3D(0, 0, 1);

}
