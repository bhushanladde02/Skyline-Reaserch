


/*
 * Computes the box-counting dimension of a multidimensional data set.
 * Based on a method from
 *   de Sousa, Traina, Traina, Wu, and Faloutsos:
 *   A Fast and Effective Method to Find Correlations among Attributes in Databases.
 */


public class BoxCountingDimension {
    // CONFIG: Number of key bits handled by each level of the tree
    private static final int BITS_PER_LEVEL = 3;

    // DERIVED: Split degree of each node (= 2^BITS_PER_LEVEL)
    private static final int SPLIT_DEGREE = 1 << BITS_PER_LEVEL;
    // DERIVED
    private static final int CHILD_ID_MASK = SPLIT_DEGREE - 1;
    // DERIVED: Depth at which z address longs have to be refilled with new address parts
    private static final int REFILL_DEPTH = BITS_PER_LEVEL * (Long.SIZE / BITS_PER_LEVEL);

    private final int d;
    private final int numbits;
    private final int maxlength;
    private Node root;


    public static void main(String[] args) {
//	int d = 60;
//	int n = 275465;

	int d = 21;
	int n = 10000000;

	int numbits = 5; // max 23 bits can be extracted from a float in our setting

//	DataGenerator dg = new DataGeneratorUniformMarginals(new DataGeneratorFromCSVFile("data/aerialtexture.csv"));
//	DataGenerator dg = new DataGeneratorIndependent();
	DataGenerator dg = new DataGeneratorBKS01Correlated();
	PointSource data = new PointSourceRAM(d, dg.generate(d, n));
	System.out.println("Generated.");
	System.out.println();

	BoxCountingDimension t = new BoxCountingDimension(data, numbits);
	t.computeDimensionCurve();
    }

    public BoxCountingDimension(PointSource data, int numbits) {
	d = data.getD();

	if (d % BITS_PER_LEVEL != 0) {
	    throw new IllegalArgumentException("BITS_PER_LEVEL must divide d.");
	}

	root = null;
	this.numbits = numbits;
	maxlength = BITS_PER_LEVEL * (d * numbits / BITS_PER_LEVEL);

	for (float[] p : data) {
	    insertAndCount(p);
	}
    }

    public void computeDimensionCurve() {
	long[] countsSq = new long[numbits + 1];
	iterateAndCount(root, d, 0, countsSq, maxlength);

	double[] x = new double[numbits + 1];
	double[] y = new double[numbits + 1];
	System.out.println("The slope of the linear part of the following curve is the box-counting dimension of the data set.");
	System.out.println();
	System.out.println("  x         y            Current slope");
	System.out.println("======================================");
	for (int i = 0; i <= numbits; i++) {
	    x[i] = -i;
	    y[i] = Math.log(countsSq[i]) / Math.log(2);
	    if (i > 0) {
		double currentSlope = (y[i - 1] - y[i]) / (x[i - 1] - x[i]);
		System.out.format("% 3.0f   %2.4f            %.4f%n", x[i], y[i], currentSlope);
	    } else {
		System.out.format("% 3.0f   %2.4f%n", x[i], y[i]);
	    }
	}
    }

    private static void iterateAndCount(Node node, int d, int depth, long[] countsSq, int maxlength) {
	if (depth % d == 0) {
	    int r = depth / d;
	    countsSq[r] += (long)node.counter * (long)node.counter;
	}
	if (depth != maxlength) {
	    for (int i = 0; i < SPLIT_DEGREE; i++) {
		if (node.children[i] != null) {
		    iterateAndCount(node.children[i], d, depth + BITS_PER_LEVEL, countsSq, maxlength);
		}
	    }
	}
    }

    private void insertAndCount(float[] p) {
	if (root == null) {
	    root = new Node();
	}
	long zaPrem = Long.reverse(NumberUtils.unsignedZAddress(p));
	int depth = 0;
	Node node = root;
	while (depth != maxlength) {
	    node.counter++;
	    if ((depth != 0) && (depth % REFILL_DEPTH == 0)) {
		zaPrem = Long.reverse(NumberUtils.unsignedZAddressStartingAt(p, depth));
	    }
	    int childIdOfNode = (int)(zaPrem & CHILD_ID_MASK);
	    zaPrem >>>= BITS_PER_LEVEL;
	    if (node.children[childIdOfNode] == null) {
		node.children[childIdOfNode] = new Node();
	    }
	    node = node.children[childIdOfNode];
	    depth += BITS_PER_LEVEL;
	}
	node.counter++;
    }



    private static class Node {
	private final Node[] children = new Node[SPLIT_DEGREE];
	private int counter = 0;
    }
}