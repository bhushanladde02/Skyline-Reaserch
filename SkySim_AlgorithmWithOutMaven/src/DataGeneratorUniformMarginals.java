

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/*
 * This is a wrapper around some DataGenerator that
 * transforms the generated data to uniform marginals.
 */

public class DataGeneratorUniformMarginals implements DataGenerator {

    private final DataGenerator dg;

    private float[] dataUnif = null;

    public DataGeneratorUniformMarginals(DataGenerator dg) {
	this.dg = dg;
    }

    @Override
    public void resetToDefaultSeed() {
	dg.resetToDefaultSeed();
    }

    @Override
    public float[] generate(int d, int n) {
	if (!dg.providesRandomData()) {
	    if (dataUnif == null) {
		float[] data = dg.generate(d, n);
		dataUnif = uniformalizeMargins(data, d, n);
	    }
	    return Arrays.copyOf(dataUnif, dataUnif.length);
	} else {
	    float[] data = dg.generate(d, n);
	    return uniformalizeMargins(data, d, n);
	}
    }

    @Override
    public float[] generate(int d, int n, int[] levels) {
	if (levels == null) {
	    return generate(d, n);
	} else {
	    throw new UnsupportedOperationException("Not supported yet.");
	}
    }

    @Override
    public float[] generate(int d, int n, int levels) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void generate(int d, int n, File file, int bytesPerRecord) throws IOException {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void generate(int d, int n, int[] levels, File file, int bytesPerRecord) throws IOException {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void generate(int d, int n, int levels, File file, int bytesPerRecord) throws IOException {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getShortName() {
	return dg.getShortName() + "_UnifMarginals";
    }

    private static float[] uniformalizeMargins(float[] data, int d, int n) {
	float[] dataUnif = new float[n * d];

	// Determine ranks for each dimension and create new data set.
	for (int dim = 0; dim < d; dim++) {
	    Integer[] ranking = new Integer[n];
	    for (int i = 0; i < n; i++) {
		ranking[i] = i;
	    }
	    Arrays.sort(ranking, new FloatArrayComparator(data, d, dim));
	    for (int rank = 0; rank < n; rank++) {
		// @ranking[rank] is the id of the @rank-th largest coordinate in dimension @dim.
		int pos = d * ranking[rank] + dim;
		if (rank > 0) {
		    int posPrev = d * ranking[rank - 1] + dim;
		    if (data[pos] == data[posPrev]) {
			// Same value ==> same normalized rank.
			dataUnif[pos] = dataUnif[posPrev];
			continue;
		    }
		}
		dataUnif[pos] = (float)rank / n;
	    }
	}

	return dataUnif;
    }

    @Override
    public boolean providesRandomData() {
	return dg.providesRandomData();
    }

    private static class FloatArrayComparator implements Comparator<Integer> {

	private final float[] data;
	private final int d;
	private final int currentDim;

	private FloatArrayComparator(float[] data, int d, int currentDim) {
	    this.data = data;
	    this.d = d;
	    this.currentDim = currentDim;
	}

	@Override
	public int compare(Integer o1, Integer o2) {
	    float f1 = data[o1 * d + currentDim];
	    float f2 = data[o2 * d + currentDim];
	    return Float.compare(f1, f2);
	}
    }
}
