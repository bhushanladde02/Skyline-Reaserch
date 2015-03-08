

// Uniform distribution on a hollow unit ball (annulus) with radius r.
// For r = 1, all points are located on the unit hypersphere.
public class DataGeneratorUnitAnnulusUnif extends AbstractDataGenerator {

    private double r;

    public DataGeneratorUnitAnnulusUnif(double r) {
        super();
        this.r = r;
    }

    @Override
    public float[] generate(int d, int n) {
	// Generation using the rejection method
	float[] data = new float[d * n];
	for (int pos = 0; pos < d * n; pos += d) {
	    while (true) {
		// Generate uniform
		float[] p = generateUniform(d, 1);
		double rP = 0;
		for (int j = 0; j < d; j++) {
		    rP += p[j] * p[j];
		}
		rP = Math.sqrt(rP);
		if (rP >= r) {
		    // Accept.
		    System.arraycopy(p, 0, data, pos, d);
		    break;
		}
	    }
	}
        return data;
    }

    @Override
    public String getShortName() {
        return "d_Annulus_Unif_" + r;
    }

    @Override
    public boolean providesRandomData() {
	return true;
    }
}
