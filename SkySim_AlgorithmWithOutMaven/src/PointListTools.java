

import java.util.Arrays;
import java.util.List;

public class PointListTools {
    public static float[] toFlatArray(List<float[]> list, int d) {
	int m = list.size();
	float[] result = new float[m * d];
	int i = 0;
	for (float[] point : list) {
	    System.arraycopy(point, 0, result, i, d);
	    i += d;
	}
	return result;
    }

    public static final PointList arrayToPointList(float[] data, int n, int d) {
	PointList result = new LinkedPointList(d);
	int nd = n * d;
	for (int i = 0; i < nd; i += d) {
	    float[] p = Arrays.copyOfRange(data, i, i + d);
	    result.add(p);
	}
	return result;
    }
}