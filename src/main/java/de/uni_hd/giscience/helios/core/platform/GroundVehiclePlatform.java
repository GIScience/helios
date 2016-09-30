package de.uni_hd.giscience.helios.core.platform;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.Directions;

public class GroundVehiclePlatform extends SimplePhysicsPlatform {

	double mEngineForceMax = 0.2;
	double mEngineForceCurrent = 0;
	double mEngineForceTarget = 0;

	double mComplexTurnThreshold_rad = Math.PI / 5;
	
	int mTurnMode = 0;

	Vector3D mTempWaypoint = null;

	public GroundVehiclePlatform() {
		super();
		
		// Disable gravity:
		mCfg_g_accel = new Vector3D(0, 0, 0);
	}

	@Override
	public void doControlStep(int simFrequency_hz) {

		// ################## BEGIN Steering and engine power #################

		double headingChange_rad = 0;

		// Normal forward driving and steering for wide-angle curves:
		if (mTurnMode == 0) {

			if (cached_vectorToTarget_xy.getNorm() > 0) {

				Rotation r1 = new Rotation(cached_vectorToTarget_xy, cached_dir_current_xy);

				double angle = r1.getAngle();

				mEngineForceTarget = 0.02;

				if (angle < mComplexTurnThreshold_rad) {
					double sign = (r1.getAxis().getZ() < 0) ? 1 : -1;
					headingChange_rad += 0.1 * sign * this.getVelocity().getNorm();

				} else {
					mTurnMode = 1;
					System.out.println("Turn mode 1");
					mTempWaypoint = getPosition().add(cached_dir_current.scalarMultiply(1));
				}
			}
		}

		// Two-step turn for narrow-angle curves, step 1:
		else if (mTurnMode == 1) {

			if (getPosition().distance(mTempWaypoint) < 0.5) {
				mTurnMode = 2;
				System.out.println("Turn mode 2");
			}
		}

		// Two-step turn for narrow-angle curves, step 2:
		else if (mTurnMode == 2) {

			Rotation r1 = new Rotation(cached_vectorToTarget_xy, cached_dir_current_xy);
						
			double angle = r1.getAngle();

			if (angle < mComplexTurnThreshold_rad) {
				mTurnMode = 0;
			}
			else {
				double sign = (r1.getAxis().getZ() < 0) ? -1 : 1;
				headingChange_rad -= 0.25 * sign * this.getVelocity().getNorm();
				mEngineForceTarget = -0.01;	
			}
		}
		// ################## END Steering and engine power #################

		// Set platform attitude:
		this.setAttitude(new Rotation(Directions.up, headingChange_rad).applyTo(getAttitude()));

		// ########## BEGIN Set engine force #########
		mEngineForceCurrent += Math.signum(mEngineForceTarget - mEngineForceCurrent) * 0.0001;

		// Limit engine forward/backward force:
		if (mEngineForceCurrent > mEngineForceMax) {
			mEngineForceCurrent = mEngineForceMax;
		}

		if (mEngineForceCurrent < -mEngineForceMax) {
			mEngineForceCurrent = -mEngineForceMax;
		}

		if (cached_dir_current.getNorm() > 0) {
			mEngineForce = cached_dir_current_xy.normalize().scalarMultiply(mEngineForceCurrent);
			
		} else {
			mEngineForce = new Vector3D(0, 0, 0);
		}
		// ########## END Set engine force #########
	}

	@Override
	public void setDestination(Vector3D dest) {
		super.setDestination(dest);
		mTurnMode = 0;
	}
}
