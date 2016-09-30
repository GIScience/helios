package de.uni_hd.giscience.helios.core.scene;

import java.nio.file.Path;
import java.util.Arrays;

public class Material {

	public String name = "default";

	public boolean isGround = true;
	public boolean useVertexColors = false;
	public int castShadows = 0;
	public int receiveShadows = 0;

	public Path matFilePath = null;

	public String map_Kd = "";
	public double reflectance = 0;

	public float[] ka = { 0, 0, 0, 0 };
	public float[] kd = { 0, 0, 0, 0 };
	public float[] ks = { 0, 0, 0, 0 };

	public float[] getKd(float factor) {
		float[] result = new float[3];
		result[0] = kd[0] * factor;
		result[1] = kd[1] * factor;
		result[2] = kd[2] * factor;

		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + castShadows;
		result = prime * result + (isGround ? 1231 : 1237);
		result = prime * result + Arrays.hashCode(ka);
		result = prime * result + Arrays.hashCode(kd);
		result = prime * result + Arrays.hashCode(ks);
		result = prime * result + ((map_Kd == null) ? 0 : map_Kd.hashCode());
		result = prime * result + ((matFilePath == null) ? 0 : matFilePath.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + receiveShadows;
		long temp;
		temp = Double.doubleToLongBits(reflectance);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (useVertexColors ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Material other = (Material) obj;
		if (castShadows != other.castShadows)
			return false;
		if (isGround != other.isGround)
			return false;
		if (!Arrays.equals(ka, other.ka))
			return false;
		if (!Arrays.equals(kd, other.kd))
			return false;
		if (!Arrays.equals(ks, other.ks))
			return false;
		if (map_Kd == null) {
			if (other.map_Kd != null)
				return false;
		} else if (!map_Kd.equals(other.map_Kd))
			return false;
		if (matFilePath == null) {
			if (other.matFilePath != null)
				return false;
		} else if (!matFilePath.equals(other.matFilePath))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (receiveShadows != other.receiveShadows)
			return false;
		if (Double.doubleToLongBits(reflectance) != Double.doubleToLongBits(other.reflectance))
			return false;
		if (useVertexColors != other.useVertexColors)
			return false;
		return true;
	}

}
