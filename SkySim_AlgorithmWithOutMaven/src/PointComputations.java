

public class PointComputations {
    public static float getVolume(float[] p) {
	float vol = 1;
	int i = p.length;
	while (--i >= 0) {
		vol *= p[i];
	}
	return vol;
    }

    public static float getSum(float[] p) {
	float sum = 0;
	int i = p.length;
	while (--i >= 0) {
		sum += p[i];
	}
	return sum;
    }
}
