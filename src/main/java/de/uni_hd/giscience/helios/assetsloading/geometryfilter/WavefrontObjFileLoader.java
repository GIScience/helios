package de.uni_hd.giscience.helios.assetsloading.geometryfilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Color4f;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import de.uni_hd.giscience.helios.assetsloading.MaterialsFileReader;
import de.uni_hd.giscience.helios.assetsloading.ScenePart;
import de.uni_hd.giscience.helios.core.scene.Material;
import de.uni_hd.giscience.helios.core.scene.primitives.Triangle;
import de.uni_hd.giscience.helios.core.scene.primitives.Vertex;

public class WavefrontObjFileLoader extends AbstractGeometryFilter {

	String filePathString = "";

	public WavefrontObjFileLoader() {
	}

	public ScenePart run() {

		boolean yIsUp = false;
		int castShadows = 0;
		int receiveShadows = 0;

		// ######### BEGIN Read up axis ###########
		String upAxis = (String) params.get("up");

		if (upAxis != null && upAxis.equals("y")) {
			yIsUp = true;
		}
		// ######### END Read up axis ###########

		// ########### BEGIN Read shadow cast & receive ##########

		Boolean blubb = (Boolean) params.get("castShadows");
		Boolean blabb = (Boolean) params.get("receiveShadows");

		if (blubb != null) {
			System.out.println("Cast shadows defined in XML.");
			castShadows = blubb ? 1 : -1;
		}

		if (blabb != null) {
			System.out.println("Receive shadows defined in XML.");
			receiveShadows = blabb ? 1 : -1;
		}

		System.out.println("Cast shadows: " + castShadows);
		System.out.println("Rec. shadows: " + receiveShadows);

		// ########### END Read shadow cast & receive ##########

		filePathString = (String) params.get("filepath");

		Path filePath = Paths.get(filePathString);

		System.out.println("Reading 3D model from .obj file '" + filePathString + "'...");

		File f = new File(filePathString);

		if (!f.exists()) {
			System.out.println("File not found: " + filePathString);
			return null;
		}

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePathString));
		} catch (FileNotFoundException e) {
			System.out.println("Failed to create buffered reader for file: " + filePathString);

			return null;
		}

		ArrayList<Vertex> vertices = new ArrayList<>();
		ArrayList<Vector3D> normals = new ArrayList<>();
		ArrayList<Vector2D> texcoords = new ArrayList<>();

		HashMap<Vertex, Vertex> vertSet = new HashMap<>();

		String currentMat = "default";

		// log.info("Adding defalt material");
		Material mat = new Material();
		mat.useVertexColors = true;
		materials.put("default", mat);

		String line;

		try {
			while ((line = br.readLine()) != null) {

				line = line.trim();

				if (line.length() == 0) {
					continue;
				}

				if (line.substring(0, 1).equals("#")) {
					continue;
				}

				String[] lineParts = line.split("\\s+");

				// ########## BEGIN Read vertex ##########
				if (lineParts[0].equals("v") && lineParts.length >= 4) {

					Vertex v = new Vertex();

					// Read position:

					double x = 0, y = 0, z = 0;

					if (yIsUp) {
						x = Double.parseDouble(lineParts[1]);
						y = -Double.parseDouble(lineParts[3]);
						z = Double.parseDouble(lineParts[2]);
					} else {
						x = Double.parseDouble(lineParts[1]);
						y = Double.parseDouble(lineParts[2]);
						z = Double.parseDouble(lineParts[3]);
					}

					v.pos = new Vector3D(x, y, z);

					// ######## BEGIN Read vertex color #########
					if (lineParts.length >= 7) {
						float r = 1, g = 1, b = 1;

						r = Float.parseFloat(lineParts[4]);
						g = Float.parseFloat(lineParts[5]);
						b = Float.parseFloat(lineParts[6]);

						Color4f color = new Color4f(r, g, b, 1);

						v.color = color;
					}
					// ######## END Read vertex color #########

					// Add vertex to vertex list:
					vertices.add(v);
				}
				// ########## END Read vertex ##########

				// ############ BEGIN Read normal vector ##############
				else if (lineParts[0].equals("vn") && lineParts.length >= 4) {

					double x = 0, y = 0, z = 0;

					if (yIsUp) {
						x = Double.parseDouble(lineParts[1]);
						y = -Double.parseDouble(lineParts[3]);
						z = Double.parseDouble(lineParts[2]);
					} else {
						x = Double.parseDouble(lineParts[1]);
						y = Double.parseDouble(lineParts[2]);
						z = Double.parseDouble(lineParts[3]);
					}

					normals.add(new Vector3D(x, y, z));
				}
				// ############ END Read normal vector ##############

				// Read texture coordinates:
				else if (lineParts[0].equals("vt") && lineParts.length >= 3) {
					Vector2D tc = new Vector2D(Double.parseDouble(lineParts[1]), Double.parseDouble(lineParts[2]));
					texcoords.add(tc);
				}

				// Read face:
				else if (lineParts[0].equals("f")) {

					// ######### BEGIN Read triangle or quad ##############
					if (lineParts.length >= 4 && lineParts.length <= 5) {

						Vertex[] v = new Vertex[4];

						boolean has_invalid_vertex = false;

						for (int ii = 0; ii < lineParts.length - 1; ii++) {

							v[ii] = new Vertex();

							String[] sv = lineParts[ii + 1].split("/");

							int vidx = 0;
							int nidx = 0;
							int tidx = 0;

							// Try to read vertex position, texture and normal indices:
							try {
								vidx = Integer.parseInt(sv[0]);
							} catch (Exception e) {
								// System.out.println("Exception during attempt to read triangle: " + e.getMessage());
								has_invalid_vertex = true;
							}

							try {
								tidx = Integer.parseInt(sv[1]);
							} catch (Exception e) {
								// System.out.println("Exception during attempt to read triangle: " + e.getMessage());
							}

							try {
								nidx = Integer.parseInt(sv[2]);
							} catch (Exception e) {
								// System.out.println("Exception during attempt to read triangle: " + e.getMessage());
							}

							// Set vertex position and color:
							if (vidx >= 1 && vertices.size() >= vidx) {
								v[ii].pos = vertices.get(vidx - 1).pos;
								v[ii].color = vertices.get(vidx - 1).color;
							} else {
								// System.out.println("Invalid vertex index: " + vidx);
							}

							// Set vertex normal:
							if (nidx >= 1 && normals.size() >= nidx) {
								v[ii].normal = normals.get(nidx - 1);
							} else {
								// System.out.println("Invalid normal index: " + nidx);
							}

							// Set vertex texture coordinates:
							if (tidx >= 1 && texcoords.size() >= tidx) {
								v[ii].texcoords = texcoords.get(tidx - 1);
							} else {
								// System.out.println("Invalid texcoords index: " + tidx);
							}

							// #### BEGIN Prevent multiple instances of same vertex ####
							if (vertSet.containsKey(v[ii])) {
								v[ii] = vertSet.get(v[ii]);
							} else {
								vertSet.put(v[ii], v[ii]);
							}
							// #### END Prevent multiple instances of same vertex ####
						}

						if (has_invalid_vertex) {
							continue;
						}

						// Read a triangle:
						if (lineParts.length == 4) {
							Triangle tri = new Triangle(v[0], v[1], v[2]);
							tri.material = getMaterial(currentMat);

							if (tri.material.castShadows == 0) {
								tri.material.castShadows = castShadows;
							}

							if (tri.material.receiveShadows == 0) {
								tri.material.receiveShadows = receiveShadows;
							}

							primsOut.mPrimitives.add(tri);
						} 
						
						// Read a quad (two triangles):
						else if (lineParts.length == 5) {
							Triangle tri1 = new Triangle(v[0], v[1], v[2]);
							tri1.material = getMaterial(currentMat);

							if (tri1.material.castShadows == 0) {
								tri1.material.castShadows = castShadows;
							}

							if (tri1.material.receiveShadows == 0) {
								tri1.material.receiveShadows = receiveShadows;
							}

							primsOut.mPrimitives.add(tri1);

							Triangle tri2 = new Triangle(v[0], v[2], v[3]);
							tri2.material = getMaterial(currentMat);

							if (tri2.material.castShadows == 0) {
								tri2.material.castShadows = castShadows;
							}

							if (tri2.material.receiveShadows == 0) {
								tri2.material.receiveShadows = receiveShadows;
							}

							primsOut.mPrimitives.add(tri2);
						}
					} else {
						System.out.println("Unsupported primitive!");
					}
					// ### END Read triangle or quad ###
				}

				// ######### BEGIN Load materials from materials file ########
				else if (lineParts[0].equals("mtllib")) {
					materials.putAll(MaterialsFileReader.loadMaterials(filePath.getParent().toString() + "/" + lineParts[1]));
				}
				// ######### END Load materials from materials file ########

				else if (lineParts[0].equals("usemtl")) {
					currentMat = lineParts[1];
				}

				else if (lineParts[0].equals("s")) {
					// TODO 4: What?
				}

				else {
					System.out.println("Unknown line: " + line);
				}
			}

			System.out.println("# primitives loaded: " + primsOut.mPrimitives.size());

		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Boolean rvn = (Boolean) params.get("recomputeVertexNormals");

		if (rvn != null && rvn == true) {
			// TODO 5: Find out why this does really weird things (distorted triangles) when applied to
			// a mesh that already has correct vertex normals (e.g. one of the nice big houses)
			primsOut.smoothVertexNormals();
		}

		return primsOut;
	}
}
