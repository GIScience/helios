package de.uni_hd.giscience.helios.core.platform;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.Directions;

public class HelicopterPlatform extends SimplePhysicsPlatform {

	double cfg_ef_xy_step = 0.001;
	double cfg_ef_z_step = 0.0001;
	double cfg_target_slowdown_xy = 0.01;
	double cfg_slowdown_dist_xy = 10;
	double cfg_accel_tilt = 2;
	double ef_xy_max = 0.1;
	double heading_rad = 0;
	double blubbSpeed = 0;
	
	Vector3D speed_xy = new Vector3D(0, 0, 0);

	Rotation r = new Rotation(new Vector3D(1, 0, 0), 0);

	@Override
	public void doControlStep(int simFrequency_hz) {

		// ############# BEGIN Set lift/sink rate #############
		double zForceTarget = 0;

		// If destination is below:
		if (cached_vectorToTarget.getZ() < 0) {

			zForceTarget = -0.1;

			// Decrease descend speed when approaching waypoint from above:
			if (cached_vectorToTarget.getZ() < 15) {
				zForceTarget = cached_vectorToTarget.getZ() * 0.005;
			}

		} else {

			// If destination is above:
			zForceTarget = 0.1;
		}

		// ############# END Set lift/sink rate #############

		//########### BEGIN Set x/y speed ##############
		speed_xy = cached_vectorToTarget.normalize();
		
		if (cached_vectorToTarget_xy.getNorm() < cfg_slowdown_dist_xy) {
			speed_xy = cached_vectorToTarget_xy.scalarMultiply(cfg_target_slowdown_xy).add(speed_xy.scalarMultiply(0.01));
		}
		
		// Limit engine power:
		if (speed_xy.getNorm() > ef_xy_max) {
			speed_xy = speed_xy.normalize().scalarMultiply(ef_xy_max);
		}
		//########### END Set x/y speed ##############
		
		// Set engine force vector:
		double ef_z = -mCfg_g_accel.getZ() + zForceTarget;

		mEngineForce = new Vector3D(speed_xy.getX(), speed_xy.getY(), ef_z);

		
		// ###################### BEGIN Set attitude ########################
		// Acceleration tilt:
		Rotation newAttitude = new Rotation(Directions.right, -mEngineForce.getY() * cfg_accel_tilt)
				.applyTo(new Rotation(Directions.forward, mEngineForce.getX() * cfg_accel_tilt));

		// ########### BEGIN Yaw rotation into movement direction ##############
		try {
			Vector3D vel_xy = new Vector3D(getVelocity().getX(), getVelocity().getY(), 0).normalize();
			Rotation r1 = new Rotation(vel_xy, cached_dir_current_xy);

			double sign = (r1.getAxis().getZ() < 0) ? -1 : 1;
			double rotSpeed = 1.5;

			heading_rad += (rotSpeed * sign) / simFrequency_hz;

			r = new Rotation(newAttitude.applyTo(Directions.up), heading_rad);
			this.setAttitude(r.applyTo(newAttitude));
		} catch (Exception e) {

		}
		// ########### END Yaw rotation into move direction ##############

		// ###################### END Set attitude ########################
	}
}
