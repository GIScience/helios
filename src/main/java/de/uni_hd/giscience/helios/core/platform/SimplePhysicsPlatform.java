package de.uni_hd.giscience.helios.core.platform;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class SimplePhysicsPlatform extends MovingPlatform {
	


	// Physics model state variables
	protected Vector3D mEngineForce = new Vector3D(0, 0, 0);

	public double mCfg_drag = 1;

	protected Vector3D mCfg_g_accel = new Vector3D(0, 0, -9.81);	
	
	void doPhysicsStep(int simFrequency_hz) {

		// ############## BEGIN Update vehicle position #################

		Vector3D drag_accel = this.getVelocity().scalarMultiply(mCfg_drag * simFrequency_hz);
		Vector3D accel = mCfg_g_accel.add(mEngineForce).subtract(drag_accel);
		Vector3D delta_v = accel.scalarMultiply(1.0 / simFrequency_hz);

		this.setVelocity(this.getVelocity().add(delta_v));
		// NOTE: Update of position happens in Platform base class

		// ################ BEGIN Check vertical distance to ground (EXPENSIVE!) ###############
		/*
		if (mCheckGround) {

			Vector3D pos = getPosition();

			Vector3D ground = scene.getGroundPointAt(pos);

			mIsOnGround = (ground != null && pos.getZ() < ground.getZ());

			if (mIsOnGround) {
				setPosition(new Vector3D(pos.getX(), pos.getY(), ground.getZ()));

				Vector3D v = getVelocity();

				if (v.getNorm() > 0 && v.getZ() < 0) {
					setVelocity(new Vector3D(v.getX(), v.getY(), 0));
				}
			}
		}
		*/
		// ################ END Check vertical distance to ground (EXPENSIVE!) ###############

		// ############## END Update vehicle position #################
	}

	@Override
	public void doSimStep(int simFrequency_hz) {
		super.doSimStep(simFrequency_hz);

		doControlStep(simFrequency_hz);
		doPhysicsStep(simFrequency_hz);
	}

	public void doControlStep(int simFrequency_hz) {

	}
}
