package de.uni_hd.giscience.helios.core.platform;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.Directions;

public class MovingPlatform extends Platform {

	private Vector3D velocity = new Vector3D(0, 0, 0);

	@Override
	public void applySettings(PlatformSettings settings, boolean manual) {
		cfg_settings_movePerSec_m = settings.movePerSec_m;

		// Set platform position:
		if (manual) {
			setPosition(settings.getPosition());
		}
	}


	@Override
	public void doSimStep(int simFrequency_hz) {
		if (this.getVelocity().getNorm() > 0) {
			this.setPosition(this.getPosition().add(this.getVelocity()));
		}
	}

	
	public Vector3D getVelocity() {
		return this.velocity;
	}

	@Override
	public void initLegManual() {

		// ########## BEGIN Set Platform Orientation towards destination #############
		try {

			Double angle = Vector3D.angle(cached_vectorToTarget_xy, cached_dir_current_xy);

			float stepSize = 0.025f;

			float heading_rad = 0;

			while (angle > stepSize) {

				heading_rad += angle;

				Rotation r = new Rotation(Directions.up, heading_rad);

				this.setAttitude(r);

				angle = Vector3D.angle(cached_vectorToTarget_xy, cached_dir_current_xy);
			}

			System.out.println("Updated orientation");

		} catch (Exception e) {

		}
		// ########## END Set Platform Orientation towards destination #############

	}

	public void setVelocity(Vector3D v) {
		this.velocity = v;
	}

	@Override
	public boolean waypointReached() {
		// TODO 5: Make waypoint tolerance configurable

		boolean result = cached_vectorToTarget.getNorm() < 0.3;

		if (result)
			System.out.println("Waypoint reached!");
		return result;
	}

}
