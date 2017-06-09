package de.uni_hd.giscience.helios.core.platform;

// TODO 3: Move some stuff from Platform to MovingPlatform

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import de.uni_hd.giscience.helios.Directions;
import de.uni_hd.giscience.helios.core.Asset;
import de.uni_hd.giscience.helios.core.scene.Scene;

public class Platform extends Asset {

	// ############ BEGIN platform configuration ############
	// Scanner mount position, attitude and rotation:
	public Vector3D cfg_device_relativeMountPosition = new Vector3D(0, 0, 0);
	public Rotation cfg_device_relativeMountAttitude = new Rotation(new Vector3D(0, 1, 0), 0);
	public String cfg_device_visModelPath = "";
	// ############ END platform configuration ############

	// ############ BEGIN misc stuff ############
	public double lastCheckZ = 0;
	public Vector3D lastGroundCheck = new Vector3D(0, 0, 0);
	public Scene scene = null;

	// Output file writer stuff:
	PrintWriter positionsFileWriter = null;
	Vector3D prevWrittenPos = new Vector3D(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
	// ############ END misc stuff ############

	// ############### BEGIN Platform Settings ##############
	public double cfg_settings_movePerSec_m = 0;
	Vector3D cfg_settings_nextWaypointPosition = new Vector3D(0, 0, 0);
	// ############### END Platform Settings ##############

	// ############# BEGIN State variables ###############
	private Vector3D position = new Vector3D(0, 0, 0);
	private Rotation attitude = new Rotation(new Vector3D(0, 0, 1), 0);

	
	

	boolean mSetOrientationOnLegInit = false;

	Vector3D cached_absoluteMountPosition = new Vector3D(0, 0, 0);
	Rotation cached_absoluteMountAttitude = new Rotation(new Vector3D(0, 1, 0), 0);

	Vector3D cached_dir_current = new Vector3D(0, 0, 0);
	Vector3D cached_dir_current_xy = new Vector3D(0, 0, 0);

	Vector3D cached_vectorToTarget = new Vector3D(0, 0, 0);
	Vector3D cached_vectorToTarget_xy = new Vector3D(0, 0, 0);
	// ############# END State variables #############

	public void applySettings(PlatformSettings settings, boolean manual) {
		cfg_settings_movePerSec_m = settings.movePerSec_m;

		// Set platform position:		
		setPosition(settings.getPosition());		
	}

	public void doSimStep(int simFrequency_hz) {		
	}

	public Rotation getAbsoluteMountAttitude() {
		return cached_absoluteMountAttitude;
	}

	public Vector3D getAbsoluteMountPosition() {
		return cached_absoluteMountPosition;
	}

	public Rotation getAttitude() {
		return this.attitude;
	}

	public Vector3D getPosition() {
		return this.position;
	}

	public Vector3D getVectorToTarget() {
		return cached_vectorToTarget;
	}

	

	public void initLegManual() {
		
	}

	public void setAttitude(Rotation attitude) {
		this.attitude = attitude;

		this.cached_absoluteMountAttitude = this.attitude.applyTo(this.cfg_device_relativeMountAttitude);

		// TODO 3: Don't call updateCachedVectors() multiple times in one sim step. Use a "dirty" flag instead
		// and call it only once
		updateCachedVectors();
	}

	public void setDestination(Vector3D dest) {
		this.cfg_settings_nextWaypointPosition = dest;

		updateCachedVectors();
	}

	public void setOutputFilePath(String path) {

		if (path.equals("")) {
			positionsFileWriter = null;
			return;
		}

		try {
			File outputFilePath = new File(path);
			outputFilePath.getParentFile().mkdirs();

			positionsFileWriter = new PrintWriter(path);
		} catch (FileNotFoundException e) {
			positionsFileWriter = null;
		}
	}

	public void setPosition(Vector3D pos) {

		this.position = pos;

		this.cached_absoluteMountPosition = this.position.add(cfg_device_relativeMountPosition);

		updateCachedVectors();

		// Write scanner position to output file:
		if (positionsFileWriter != null) {

			if (prevWrittenPos.distance(pos) > 0.01) {

				String colorString = "";

				String formatString = "%.3f";

				Vector3D shifted = pos.add(scene.getShift());

				String x = String.format(formatString, shifted.getX());
				String y = String.format(formatString, shifted.getY());
				String z = String.format(formatString, shifted.getZ());

				positionsFileWriter.println(x + " " + y + " " + z + colorString);
				positionsFileWriter.flush();

				prevWrittenPos = new Vector3D(pos.getX(), pos.getY(), pos.getZ());
			}
		}
	}

	
	void updateCachedVectors() {
		cached_vectorToTarget = this.cfg_settings_nextWaypointPosition.subtract(this.position);
		cached_vectorToTarget_xy = new Vector3D(cached_vectorToTarget.getX(), cached_vectorToTarget.getY(), 0);
	
		cached_dir_current = this.attitude.applyTo(Directions.FORWARD);
		cached_dir_current_xy = new Vector3D(cached_dir_current.getX(), cached_dir_current.getY(), 0).normalize();
	}

	public boolean waypointReached() {

		// Stationary platforms are always at their destination:
		return true;
	}
}
