package de.uni_hd.giscience.helios.visualization.appStates;

import java.nio.FloatBuffer;

import javax.vecmath.Color4f;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import de.uni_hd.giscience.helios.core.scanner.Measurement;
import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;
import sebevents.EventListener;
import sebevents.SebEvents;

public class ShowPointCloudAppState extends BaseAppState implements EventListener {

	public ShowPointCloudAppState(SurveyPlayback sim) {
		super(sim);
	}

	private int cfg_pointCloudBufferSize = 5000000;

	double mPointDistOffset = 0.1;
	
	Vector3f newPointPos = null;

	int lastVisualizedPointIndex = 0;
	// private VertexBuffer pointCloudVB = null;

	private Mesh pointCloudMesh = null;
	private Geometry pointCloudGeometry = null;
	private int pointCloudVboAddIndex = 0;

	// ##### BEGIN Action listeners ######
	private ActionListener actionListener = new ActionListener() {

		public void onAction(String name, boolean keyPressed, float tpf) {

			if (keyPressed) {

				switch (name) {
				case "clearPointCloud":
					clearPointCloud();
					break;

				}

			}
		}
	};

	public void clearPointCloud() {
		float[] pointPositions = new float[cfg_pointCloudBufferSize * 3];

		// Initialize point cloud coordinates with something very very far away,
		// so that the points are not visible in the beginning:
		for (int ii = 0; ii < pointPositions.length; ii++) {
			pointPositions[ii] = Float.MAX_VALUE;
		}

		int[] size = { 6 };
		float[] texcoord = { 0, 0, 1, 1 };
		pointCloudMesh.setBuffer(VertexBuffer.Type.Position, 3, pointPositions);
		pointCloudMesh.setBuffer(VertexBuffer.Type.Size, 1, size);
		pointCloudMesh.setBuffer(VertexBuffer.Type.TexCoord4, 4, texcoord);
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		SebEvents.events.addListener("cmd_clear_points_buffer", this);

		// ######################## BEGIN Add point cloud geometry ###########################
		pointCloudMesh = new Mesh();

		pointCloudMesh.setMode(Mesh.Mode.Points);

		// TODO 2: Find replacements for setPointSize() and setLineWidth()
		//pointCloudMesh.setPointSize(4);

		clearPointCloud();

		pointCloudGeometry = new Geometry("pointCloud", pointCloudMesh);

		Material pointCloudMaterial = new Material(mAssetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		Color4f col = sim.pointCloudColor;

		if (col != null) {
			pointCloudMaterial.setColor("Color", new ColorRGBA(col.x, col.y, col.z, col.w));
		} else {
			pointCloudMaterial.setColor("Color", new ColorRGBA(1, 1, 0, 1));
		}

		pointCloudGeometry.setMaterial(pointCloudMaterial);

		mWorldNode.attachChild(pointCloudGeometry);
		// ##################### END Add point cloud geometry ###########################

		inputManager.addMapping("clearPointCloud", new KeyTrigger(KeyInput.KEY_F12));

		inputManager.addListener(actionListener, "clearPointCloud");
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

	}

	@Override
	public void update(float tpf) {

		// #################### BEGIN Asynchronous solution #################

		boolean pointAdded = false;

		int lastMeasurementIndex = sim.mbuffer.getLastRecordedPointIndex();

		// Was a new point added since the previous update of the point cloud VBO?
		if (lastMeasurementIndex != lastVisualizedPointIndex) {

			VertexBuffer pointCloudVB = pointCloudMesh.getBuffer(Type.Position);
			//VertexBuffer pointSize = pointCloudMesh.getBuffer(Type.Size);

			while (true) {

				Measurement m = sim.mbuffer.getEntryAt(lastVisualizedPointIndex);

				lastVisualizedPointIndex++;

				if (lastVisualizedPointIndex >= sim.mbuffer.getSize() - 1) {
					lastVisualizedPointIndex = 0;
				}

				if (m != null) {

					newPointPos = am2jme_vector(m.beamOrigin.add(m.beamDirection.scalarMultiply(m.distance - mPointDistOffset)));

					BufferUtils.setInBuffer(newPointPos, (FloatBuffer) pointCloudVB.getData(), pointCloudVboAddIndex);

					pointCloudVboAddIndex++;

					if (pointCloudVboAddIndex == cfg_pointCloudBufferSize - 1) {
						pointCloudVboAddIndex = 0;
					}

					pointAdded = true;
				}

				if (lastVisualizedPointIndex == lastMeasurementIndex) {
					break;
				}
			}

			// #################### END Asynchronous solution #################

			// #################### BEGIN Synchronous (single-thread) solution #################
			/*
			 * lastPoint = sim.getScanner().lastPoint;
			 * 
			 * if (lastPoint != null) {
			 * 
			 * Vector3f newPoint = am2jme_vector(lastPoint.position);
			 * 
			 * BufferUtils.setInBuffer(newPoint, (FloatBuffer) pointCloudVB.getData(), pointCloudVboAddIndex);
			 * 
			 * pointCloudVboAddIndex++;
			 * 
			 * if (pointCloudVboAddIndex == cfg_pointCloudBufferSize - 1) { pointCloudVboAddIndex = 0; }
			 * 
			 * pointAdded = true; }
			 */
			// #################### END Synchronous (single-thread) solution #################

			// If a new point was added to the point cloud VBO, update the VBO:
			if (pointAdded) {
		
				// ATTENTION:
				// Apparently, pointCloudMesh.updateBound() isn't needed.
				// Not doing it gives about 50% more FPS on the Thinkpad.
				// pointCloudMesh.updateBound();
				pointCloudVB.setUpdateNeeded();
				pointCloudGeometry.updateModelBound();
			}
		}

	}

	@Override
	public void handleEvent(String eventName, Object payload) {
		// TODO Auto-generated method stub

		if (eventName.equals("cmd_clear_points_buffer")) {
			clearPointCloud();
		}
	}

}
