package de.uni_hd.giscience.helios.visualization.appStates;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

import de.uni_hd.giscience.helios.core.scene.primitives.Primitive;
import de.uni_hd.giscience.helios.core.scene.primitives.Triangle;
import de.uni_hd.giscience.helios.core.scene.primitives.Vertex;
import de.uni_hd.giscience.helios.core.scene.primitives.Voxel;
import de.uni_hd.giscience.helios.surveyplayback.SurveyPlayback;

public class ShowScannedSceneAppState extends BaseAppState {

	private Node scenePartsGroupNode = null;

	public ShowScannedSceneAppState(SurveyPlayback sim) {
		super(sim);
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		// ######################### BEGIN Add scene part geometries to the root node #####################
		// Create group node for the scene parts:
		// NOTE: This is primarily done to allow easy hiding of the scene (by detaching the scene parts group node from the root node)
		scenePartsGroupNode = new Node();
		mWorldNode.attachChild(scenePartsGroupNode);

		HashMap<de.uni_hd.giscience.helios.core.scene.Material, HashSet<Primitive>> blablubb = new HashMap<>();

		for (Primitive p : sim.getScanner().platform.scene.primitives) {

			if (!blablubb.containsKey(p.material)) {
				blablubb.put(p.material, new HashSet<Primitive>());
			}

			blablubb.get(p.material).add(p);
		}

		createGeometryFromScenePart(blablubb);
		// ########################## END Add scene part geometries to the root node #####################
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);

	}

	@Override
	public void update(float tpf) {
		// System.out.println("ssa!");
	}

	private void createGeometryFromScenePart(HashMap<de.uni_hd.giscience.helios.core.scene.Material, HashSet<Primitive>> sceneParts) {

		Iterator<Entry<de.uni_hd.giscience.helios.core.scene.Material, HashSet<Primitive>>> iter = sceneParts.entrySet().iterator();

		while (iter.hasNext()) {
			Map.Entry<de.uni_hd.giscience.helios.core.scene.Material, HashSet<Primitive>> pair = (Map.Entry<de.uni_hd.giscience.helios.core.scene.Material, HashSet<Primitive>>) iter
					.next();

			de.uni_hd.giscience.helios.core.scene.Material partMaterial = pair.getKey();
			HashSet<Primitive> scenePart = pair.getValue();

			Mesh mesh = new Mesh();

			Geometry geometry = null;
			Material mat = null;

			HashMap<Vertex, Integer> vertexMap = new HashMap<>();
			ArrayList<Vertex> vertexList = new ArrayList<>();
			ArrayList<int[]> triList = new ArrayList<>();

			int ii = 0;

			// ########## BEGIN Compile lists of required vertices and faces, making sure that they contain no duplicates #########
			for (Primitive p : scenePart) {

				if (p.getClass() == Triangle.class) {

					Triangle t = (Triangle) p;

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

				else if (p.getClass() == Voxel.class) {

					ArrayList<Vertex> verts = p.getVertices();

					if (verts != null) {
						vertexList.addAll(verts);
					}

				}
			}

			// LidarSim.log.info("# vertices in mesh: " + vertexList.size());

			// ########## END Compile lists of required vertices and faces, making sure that they contain no duplicates #########

			// ############### BEGIN Set up buffers for vertex positions, normals and colors and fill them with data ###############
			Vector3f[] positions = new Vector3f[vertexList.size()];
			Vector3f[] normals = new Vector3f[vertexList.size()];
			Vector4f[] colors = new Vector4f[vertexList.size()];
			Vector2f[] texcoords = new Vector2f[vertexList.size()];

			ii = 0;

			for (Vertex v : vertexList) {
				Vector3f originalPos = am2jme_vector(v.pos);
				positions[ii] = originalPos;

				normals[ii] = am2jme_vector(v.normal);

				if (v.color != null) {
					colors[ii] = new Vector4f((float) v.color.x, (float) v.color.y, (float) v.color.z, 1);
				}

				if (v.texcoords != null) {
					texcoords[ii] = new Vector2f((float) v.texcoords.getX(), (float) v.texcoords.getY());
				}

				ii++;
			}

			// LidarSim.log.info(ii + " JME vertices added.");

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

				// LidarSim.log.info("Mesh rendering mode: Triangles");

				mesh.setMode(Mode.Triangles);

				// ############## BEGIN Set up material #################

				String textureKey = "DiffuseMap";
				String vertexColorKey = "UseVertexColor";

				mat = new Material(mAssetManager, "Common/MatDefs/Light/Lighting.j3md");

				if (partMaterial != null) {

					// LidarSim.log.info("Applying material " + partMat.name);

					// ########### BEGIN Set material colors ##########
					ColorRGBA diffuse = new ColorRGBA(partMaterial.kd[0], partMaterial.kd[1], partMaterial.kd[2], partMaterial.kd[3]);
					ColorRGBA ambient = new ColorRGBA(partMaterial.ka[0], partMaterial.ka[1], partMaterial.ka[2], partMaterial.ka[3]);
					ColorRGBA specular = new ColorRGBA(partMaterial.ks[0], partMaterial.ks[1], partMaterial.ks[2], partMaterial.ks[3]);

					mat.setColor("Diffuse", diffuse);
					mat.setColor("Ambient", ambient);
					mat.setColor("Specular", specular);

					mat.setBoolean("UseMaterialColors", true);
					// ########### END Set material colors ##########

					// ########## BEGIN Set vertex colors #########
					if (partMaterial.useVertexColors) {
						System.out.println("Using vertex colors");
						mat.setBoolean(vertexColorKey, true);
					}
					// ########## END Set vertex colors #########

					// ########### BEGIN Set texture ##########
					String textureFileName = partMaterial.map_Kd;

					String texturePath = "";
					
					

					if (!textureFileName.equals("")) {
						try {
							System.out.println("texture path: " + texturePath);

							texturePath = partMaterial.matFilePath.toString() + File.separator + textureFileName;
							
							// TODO 2: Understand why it is looking for the *sceneparts* folder here! VERY VERY WEIRD!
							mAssetManager.registerLocator("", FileLocator.class);
							Texture tex = mAssetManager.loadTexture(texturePath.replace(File.separator, "/"));

							tex.setWrap(WrapMode.Repeat);

							mat.setTexture(textureKey, tex);

						} catch (Exception e) {
							System.out.println("Failed to load texture: " + texturePath);
						}
						
					}
					// ########### END Set texture ##########
				}

				// Fill triangle mesh and texture coordinate buffers:

				// ATTENTION: Normals/Index/TexCoord buffers must only be set of the respective lists really contain data!
				// Otherwise, the mesh will not be drawn!
				mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(triangleCornerIndexes));
				mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
				mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texcoords));

			} else {
				// LidarSim.log.info("Mesh rendering mode: Points");

				mesh.setMode(Mode.Points);
				mesh.setPointSize(4);

				mat = new Material(mAssetManager, "Common/MatDefs/Misc/Unshaded.j3md");

				mat.setBoolean("VertexColor", true);
			}

			// Fill Vertex position and color buffers:
			mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(positions));
			mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(colors));

			mesh.updateBound();

			geometry = new Geometry("", mesh);
			geometry.updateModelBound();
			geometry.setMaterial(mat);

			// ################ BEGIN Configure shadow cast/receive ##################
			if (partMaterial.castShadows == 1 && partMaterial.receiveShadows == 1) {
				geometry.setShadowMode(ShadowMode.CastAndReceive);

			} else if (partMaterial.castShadows == 1) {
				geometry.setShadowMode(ShadowMode.Cast);

			} else if (partMaterial.receiveShadows == 1) {
				geometry.setShadowMode(ShadowMode.Receive);

			} else {
				geometry.setShadowMode(ShadowMode.Off);
			}
			// ################ END Configure shadow cast/receive ##################

			scenePartsGroupNode.attachChild(geometry);
		}
	}
}
