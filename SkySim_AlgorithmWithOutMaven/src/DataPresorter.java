/*
 * sorts a List<float[]> prior to skyline computation
 */


import java.util.Arrays;
import java.util.List;

public class DataPresorter {

    public static PointSource sortByVolume(PointSource data) {
	int n = data.size();
	float[] vols = new float[n];
	for (int i = 0; i < n; i++) {
	    float[] p = data.get(i);
	    vols[i] = PointComputations.getVolume(p);
	}

	int d = data.getD();
	float[] dataA = data.toFlatArray();
	ArraySorter.floatArraySortDecreasing(dataA, d, vols);

	return new PointSourceRAM(d, dataA);

    }

    public static PointSource sortBySum(PointSource data) {
	int n = data.size();
	float[] sums = new float[n];
	for (int i = 0; i < n; i++) {
	    float[] p = data.get(i);
	    sums[i] = PointComputations.getSum(p);
	}

	int d = data.getD();
	float[] dataA = data.toFlatArray();
	ArraySorter.floatArraySortDecreasing(dataA, d, sums);

	return new PointSourceRAM(d, dataA);
    }

    public static PointSource sortByZAddress(PointSource data) {
	int n = data.size();
	long[] zas = new long[n];
	for (int i = 0; i < n; i++) {
	    float[] p = data.get(i);
	    zas[i] = NumberUtils.unsignedZAddress(p);
	}

	int d = data.getD();
	float[] dataA = data.toFlatArray();
	ArraySorter.unsignedLongArraySortDecreasing(dataA, d, zas);

	return new PointSourceRAM(d, dataA);
    }

    public static PointSource sortBestToFront(List<float[]> data, double prob) {
	System.out.format("Sorting data partially, moving most probable skyline points to front (threshold: %.3f) ... ", prob);
	int numPoints = 0;
	// sort all points to front
	// having a uniform skyline probablity of at least prob
	if (!(data instanceof PointSourceRAM)) {
	    throw new UnsupportedOperationException("Not supported yet.");
	}
	PointSourceRAM psr = (PointSourceRAM) data;
	final int d = psr.getD();
	final int n = psr.size();
	float[] dataArray = Arrays.copyOf(psr.toFlatArray(), d * n);
	float[] temp = new float[d];
	int pointerHead = 0;
	// pointerHead: where next "good" point will be inserted at head of data array
	// pointerData: current position in data array
	final double probNew = Math.pow(prob, 1 / (double) (n - 1));
	for (int pointerData = 0; pointerData < n * d; pointerData += d) {
	    double dominatingVol = 1;
	    for (int i = 0; i < d; i++) {
		dominatingVol *= 1 - dataArray[pointerData + i];
	    }
	    double probSkyUniformNew = 1 - dominatingVol;
	    if (probSkyUniformNew >= probNew) {
		numPoints++;
		// swap it
		System.arraycopy(dataArray, pointerHead, temp, 0, d);
		System.arraycopy(dataArray, pointerData, dataArray, pointerHead, d);
		System.arraycopy(temp, 0, dataArray, pointerData, d);
		pointerHead += d;
	    }
	}
	System.out.format("%d points moved%n", numPoints);
	return new PointSourceRAM(d, dataArray);
    }
}
