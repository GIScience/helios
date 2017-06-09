package de.uni_hd.giscience.helios;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/** Definition of the coordinate system convention for the lidar sim.
 *
 * x - RIGHT
 * y - FORWARD
 * z - UP
 */
public class Directions {
    public static final Vector3D FORWARD = new Vector3D(0, 1, 0);
    public static final Vector3D RIGHT = new Vector3D(1, 0, 0);
    public static final Vector3D UP = new Vector3D(0, 0, 1);
}
