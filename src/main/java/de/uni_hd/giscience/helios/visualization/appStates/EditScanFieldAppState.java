package de.uni_hd.giscience.helios.visualization.appStates;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import de.uni_hd.giscience.helios.core.scanner.Scanner;
import de.uni_hd.giscience.helios.core.scanner.ScannerSettings;
import de.uni_hd.giscience.helios.core.scene.primitives.Triangle;
import de.uni_hd.giscience.helios.core.scene.primitives.Vertex;
import de.uni_hd.giscience.helios.surveyplayback.Leg;
import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;
import sebevents.EventListener;
import sebevents.SebEvents;

public class EditScanFieldAppState extends BaseAppState implements EventListener {

	int mRotateDir = 0;
	float mRotateStep = 0.025f;

	int mGrowDir = 0;
	float mGrowStep = 0.025f;

	Geometry mScanFieldGeometry = null;

	float mHorizontalScanFieldIndicatorRadius_m = 2;
	float mHorizontalScanFieldMeshAngleStep = 0.05f;

	public Node mountNode = null;
	Node mScanFieldIndicatorGroupNode = null;

	double mScanAngleStart_last = 0;
	double mScanAngleStop_last = 0;

	// ##### BEGIN Action listeners ######
	private ActionListener mActionListener = new ActionListener() {

		public void onAction(String name, boolean keyPressed, float tpf) {

			if (!mToolEnabled) {
				return;
			}

			Leg selectedLeg = mPlayback.getCurrentLeg();

			if (keyPressed) {

				switch (name) {
				case "up":
					mGrowDir = 1;
					break;

				case "down":
					mGrowDir = -1;
					break;

				case "left":
					mRotateDir = 1;
					break;
				case "right":
					mRotateDir = -1;
					break;
				}

				sim.getScanner().applySettings(selectedLeg.mScannerSettings);
			} else {

				switch (name) {
				case "up":
					mGrowDir = 0;
					break;

				case "down":
					mGrowDir = 0;
					break;

				case "left":
					mRotateDir = 0;
					break;
				case "right":
					mRotateDir = 0;
					break;
				}
			}
		}
	};

	public EditScanFieldAppState(SurveyPlayback sim) {
		super(sim);

		SebEvents.events.addListener("simulation_pause_state_changed", this);
		

	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		// Add horizontal scan angle indicator group node:
		mScanFieldIndicatorGroupNode = new Node("horiScanIndicator");

		Node platformNode = (Node) mWorldNode.getChild("platformNode");
		mountNode = (Node) platformNode.getChild("mountNode");

		mountNode.attachChild(mScanFieldIndicatorGroupNode);

		Mesh mesh = updateScanFieldIndicatorMesh();
		mScanFieldGeometry = new Geometry("", mesh);
		mScanFieldGeometry.updateModelBound();

		Material mat = new Material(mAssetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", new ColorRGBA(0.6f, 0.6f, 1f, 0.1f));
		mScanFieldGeometry.setMaterial(mat);

		mScanFieldIndicatorGroupNode.attachChild(mScanFieldGeometry);

		inputManager.addListener(mActionListener, "up", "down", "left", "right");

	}

	@Override
	public void setToolEnabled(boolean enabled) {
	
		if (enabled) {
			sim.pause(true);
			
			mHorizontalScanFieldIndicatorRadius_m = 5;
		} else {
			mHorizontalScanFieldIndicatorRadius_m = 2;
		}
		
		updateScanFieldIndicatorMesh();
	
		super.setToolEnabled(enabled);
	}

	private Mesh updateScanFieldIndicatorMesh() {

		Mesh mesh = new Mesh();

		ArrayList<Triangle> tris = new ArrayList<>();

		Leg currentLeg = mPlayback.getCurrentLeg();

		// sin(a) = gkat / hyp
		// cos(a) = akat / hyp

		HashMap<Vertex, Integer> vertexMap = new HashMap<>();
		ArrayList<Vertex> vertexList = new ArrayList<>();
		ArrayList<int[]> triList = new ArrayList<>();

		int ii = 0;

		// Scanner:
		Vector3D v1 = new Vector3D(0, 0, 0);
		Vertex ve1 = new Vertex();
		ve1.pos = v1;

		Double angle_start = currentLeg.mScannerSettings.headRotateStart_rad + Math.PI / 2;
		Double angle_stop = currentLeg.mScannerSettings.headRotateStopInRad + Math.PI / 2;

		Double angle_diff = angle_stop - angle_start;

		int numSteps = (int) Math.ceil(angle_diff / mHorizontalScanFieldMeshAngleStep);

		for (int kk = 0; kk < numSteps; kk++) {

			Double a1 = angle_start + kk * mHorizontalScanFieldMeshAngleStep;
			Double a2 = angle_start + (kk + 1) * mHorizontalScanFieldMeshAngleStep;

			float akat1 = (float) Math.cos(a1) * mHorizontalScanFieldIndicatorRadius_m;
			float gkat1 = (float) Math.sin(a1) * mHorizontalScanFieldIndicatorRadius_m;

			float akat2 = (float) Math.cos(a2) * mHorizontalScanFieldIndicatorRadius_m;
			float gkat2 = (float) Math.sin(a2) * mHorizontalScanFieldIndicatorRadius_m;

			Vector3D v2 = new Vector3D(akat1, gkat1, 0);
			Vector3D v3 = new Vector3D(akat2, gkat2, 0);

			Vertex ve2 = new Vertex();
			ve2.pos = v2;

			Vertex ve3 = new Vertex();
			ve3.pos = v3;

			Triangle t1 = new Triangle(ve1, ve2, ve3);
			tris.add(t1);
		}

		for (Triangle t : tris) {

			int corners[] = new int[3];

			for (int ci = 0; ci <= 2; ci++) {
				Vertex v = t.verts[ci];

				// NOTE: The vertexList HashSet makes sure that we don't
				// add the same vertex to the vertexList multiple times.
				// This would happen each time when a vertex is shared by multiple triangles

				Integer index = vertexMap.get(v);

				if (index == null) {

					vertexMap.put(v, ii);
					vertexList.add(v);

					corners[ci] = ii;
					ii++;
				} else {
					corners[ci] = index;
				}
			}

			triList.add(corners);
		}

		// ############### BEGIN Set up buffers for vertex positions, normals and colors and fill them with data ###############
		Vector3f[] positions = new Vector3f[vertexList.size()];
		/*
		Vector3f[] normals = new Vector3f[vertexList.size()];
		Vector4f[] colors = new Vector4f[vertexList.size()];
		Vector2f[] texcoords = new Vector2f[vertexList.size()];
*/
		ii = 0;

		for (Vertex v : vertexList) {
			Vector3f originalPos = am2jme_vector(v.pos);
			positions[ii] = originalPos;

			/*
			 * normals[ii] = am2jme_vector(v.normal);
			 * 
			 * if (v.color != null) { colors[ii] = new Vector4f((float) v.color.x, (float) v.color.y, (float) v.color.z, 1); }
			 * 
			 * if (v.texcoords != null) { texcoords[ii] = new Vector2f((float) v.texcoords.getX(), (float) v.texcoords.getY()); }
			 */
			ii++;
		}

		// ############### END Set up buffers for vertex positions, normals and colors and fill them with data ##############

		// #### BEGIN Set up buffer for triangle vertex indices and fill it with data ####
		int[] triangleCornerIndexes = new int[triList.size() * 3];

		ii = 0;
		for (int[] tri : triList) {
			triangleCornerIndexes[ii++] = tri[0];
			triangleCornerIndexes[ii++] = tri[1];
			triangleCornerIndexes[ii++] = tri[2];
		}
		// #### END Set up buffer for triangle vertex indices and fill it with data ####

		if (triList.size() > 0) {

			mesh.setMode(Mode.Triangles);

			// ATTENTION: Normals/Index/TexCoord buffers must only be set of the respective lists really contain data!
			// Otherwise, the mesh will not be drawn!
			mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(triangleCornerIndexes));
			// mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
			// mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texcoords));

		}

		// Fill Vertex position and color buffers:
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(positions));
		// mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(colors));

		mesh.updateBound();

		if (mScanFieldGeometry != null) {
			mScanFieldGeometry.setMesh(mesh);
			mScanFieldGeometry.updateModelBound();
		}

		return mesh;
	}

	@Override
	public void update(float tpf) {
		

		Scanner currentScanner = sim.getScanner();

		Leg leg = sim.getCurrentLeg();

		ScannerSettings ss = leg.mScannerSettings;

		// ############## BEGIN What happens when there are new scanner settings ##############

		if (currentScanner != null) {

			// ############ BEGIN Set up horizontal scan field indicator ############
			Leg currentLeg = mPlayback.getCurrentLeg();

			if (mScanAngleStart_last != currentLeg.mScannerSettings.headRotateStart_rad || mScanAngleStop_last != currentLeg.mScannerSettings.headRotateStopInRad) {

				Mesh mesh = updateScanFieldIndicatorMesh();
				// mScanFieldGeometry.setMesh(mesh);
				// mScanFieldGeometry.updateModelBound();

				mScanAngleStart_last = currentLeg.mScannerSettings.headRotateStart_rad;
				mScanAngleStop_last = currentLeg.mScannerSettings.headRotateStopInRad;
			}

		}
		// ############## END What happens when there are new scanner settings ##############

		// ################# BEGIN Edit Scan Field Mode ####################

		float scanFieldHalfWidth_rad = (float) ((ss.headRotateStopInRad - ss.headRotateStart_rad) / 2);
		float scanFieldCenterAngle_rad = (float) (ss.headRotateStart_rad + scanFieldHalfWidth_rad);

		if (mGrowDir != 0) {

			scanFieldHalfWidth_rad += mGrowDir * mGrowStep;

			// Clamp:
			if (scanFieldHalfWidth_rad < 0) {
				scanFieldHalfWidth_rad = 0;
			} else if (scanFieldHalfWidth_rad > Math.PI) {
				scanFieldHalfWidth_rad = (float) Math.PI;
			}

			ss.headRotateStart_rad = (double) (scanFieldCenterAngle_rad - scanFieldHalfWidth_rad);
			ss.headRotateStopInRad = (double) (scanFieldCenterAngle_rad + scanFieldHalfWidth_rad);

			sim.getScanner().applySettings(ss);

		}

		if (mRotateDir != 0) {
			scanFieldCenterAngle_rad += mRotateDir * mRotateStep;

			ss.headRotateStart_rad = (double) (scanFieldCenterAngle_rad - scanFieldHalfWidth_rad);
			ss.headRotateStopInRad = (double) (scanFieldCenterAngle_rad + scanFieldHalfWidth_rad);

			sim.getScanner().applySettings(ss);
		}
		// ################# END Edit Scan Field Mode ####################
	}

	@Override
	public void handleEvent(String eventName, Object payload) {
		
		switch(eventName) {
		case "simulation_pause_state_changed":
			
			boolean paused = (boolean) payload;
	
			if (paused) {
				mScanFieldGeometry.setCullHint(CullHint.Inherit);
			}
			else {
				mScanFieldGeometry.setCullHint(CullHint.Always);
			}
				
			break;
	
		}
	}
}
