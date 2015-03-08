

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/*
 * This is a wrapper around some DataGenerator that
 * transforms the generated data to marginals in [0, 1] using linear scaling.
 */

public class DataGeneratorUnitMarginalsByLinearScaling implements DataGenerator {

    private final DataGenerator dg;

    private float[] dataRescaled = null;

    public DataGeneratorUnitMarginalsByLinearScaling(DataGenerator dg) {
	this.dg = dg;
    }

    @Override
    public void resetToDefaultSeed() {
	dg.resetToDefaultSeed();
    }

    @Override
    public float[] generate(int d, int n) {
	if (!dg.providesRandomData()) {
	    if (dataRescaled == null) {
		float[] data = dg.generate(d, n);
		dataRescaled = rescaleMarginals(data, d, n);
	    }
	    return Arrays.copyOf(dataRescaled, dataRescaled.length);
	} else {
	    float[] data = dg.generate(d, n);
	    return rescaleMarginals(data, d, n);
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

    private static float[] rescaleMarginals(float[] data, int d, int n) {
	float[] dataRescaled = new float[n * d];

	// Determine min and max for each dimension.
	float[] min = new float[d];
	float[] max = new float[d];
	for (int pos = 0; pos < d * n; pos += d) {
	    for (int dim = 0; dim < d; dim++) {
		float x = data[pos + dim];
		if (x < min[dim]) {
		    min[dim] = x;
		}
		if (x > max[dim]) {
		    max[dim] = x;
		}
	    }
	}
	
	// Determine scale and shift.
	float[] width = new float[d];
	float[] mean = new float[d];
	for (int dim = 0; dim < d; dim++) {
	    width[dim] = max[dim] - min[dim];
	    mean[dim] = (min[dim] + max[dim]) / 2;
	}
	
	// Make sure that a value of 1 is never reached
	// (this would disturb the computation of z addresses).
	for (int dim = 0; dim < d; dim++) {
	    width[dim] *= 1.01;
	}

	// Rescale.
	for (int pos = 0; pos < d * n; pos += d) {
	    for (int dim = 0; dim < d; dim++) {
		dataRescaled[pos + dim] = 0.5f + (data[pos + dim] - mean[dim]) / width[dim];
	    }
	}

	return dataRescaled;
    }

    @Override
    public boolean providesRandomData() {
	return dg.providesRandomData();
    }
}
