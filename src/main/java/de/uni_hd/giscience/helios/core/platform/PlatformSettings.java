package de.uni_hd.giscience.helios.core.platform;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.core.Asset;

/**
 * Platform setting
 */
public class PlatformSettings extends Asset {

	public double x = 0;
	public double y = 0;
	public double z = 0;

	public boolean onGround = false;

  /**
   * 100 meter per sec are 360 km/h
   */
  public double movePerSec_m = 70;

  /**
   * Returns start position
   * @return start position as vector
   */
  public Vector3D getPosition() {
		return new Vector3D(x, y, z);
	}

  /**
   * Sets start postion of platform
   * @param dest position
   */
  public void setPosition(Vector3D dest) {
		this.x = dest.getX();
		this.y = dest.getY();
		this.z = dest.getZ();
	}
}
