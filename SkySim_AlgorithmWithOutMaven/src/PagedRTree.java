

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import ifis.skysim2.data.generator.DataGenerator;
import ifis.skysim2.data.generator.DataGeneratorIndependent;

public class PagedRTree {
    protected static final int DEFAULT_NODE_CAPACITY_MIN = 10;
    protected static final int DEFAULT_NODE_CAPACITY_MAX = 20;

    private Node root;
    private final int d;

    public int nodeCapacityMin;
    public int nodeCapacityMax;

    public static void main(String[] args) {
	int d = 5;
	int n = 1000000;

	DataGenerator dg = new DataGeneratorIndependent();
	dg.resetToDefaultSeed();
	RandomEngine re = new MersenneTwister();
	float[] data = dg.generate(d, n);

	PagedRTree tree = new PagedRTree(d);
	float [] point = new float[d];
	int nd = n * d;

	long t = System.nanoTime();

	for (int i = 0; i < nd; i += d) {
	    System.arraycopy(data, i, point, 0, d);
	    InternalNode parent = null;
	    int k = -1;
	    Node node = tree.root;
	    while (node instanceof InternalNode) {
		k = re.nextInt() % DEFAULT_NODE_CAPACITY_MIN;
		if (k < 0) {
		    k += DEFAULT_NODE_CAPACITY_MIN;
		}
		InternalNode inode = (InternalNode)node;
		parent = inode;
		node = inode.children[k];
	    }
	    LeafNode lnode = (LeafNode)node;
	    if (node.size < DEFAULT_NODE_CAPACITY_MIN) {
		lnode.append(point);
	    } else {
		InternalNode newNode = tree.createInternalNode();
		if (k == -1) {
		    tree.root = newNode;
		} else {
		    parent.children[k] = newNode;
		}
		newNode.children[0] = node;
		for (int j = 1; j < DEFAULT_NODE_CAPACITY_MIN; j++) {
		    newNode.children[j] = tree.createLeafNode();
		}
	    }
	}

	long time = System.nanoTime() - t;
	System.out.format("%.2f ms%n", time / 1000000.0);
    }

    public PagedRTree(int d, int nodeCapacityMin, int nodeCapacityMax) {
	this.d = d;
	this.nodeCapacityMin = nodeCapacityMin;
	this.nodeCapacityMax = nodeCapacityMax;
	root = createLeafNode();
    }

    public PagedRTree(int d) {
	this(d, DEFAULT_NODE_CAPACITY_MIN, DEFAULT_NODE_CAPACITY_MAX);
    }

    private LeafNode createLeafNode() {
//	return new Node(d, true, nodeCapacityMax);
//	return new Node(true);
	return new LeafNode();
    }

    private InternalNode createInternalNode() {
//	return new Node(d, false, nodeCapacityMax);
//	return new Node(false);
	return new InternalNode();
    }

    // dominationFreeInsert (liefert true false oder int der #dom)

    // iterator


    // TODO: Performance testen: Erzeuge zufällige Bäume in verschiedenen
    // Implementierungen (traversal + insert):
    // (1) so wie hier --> 2250
    // (2) so wie hier, aber Node nicht statisch --> 2330
    // (3) Node mit zwei Unterklassen, alles static
    // (4) Node mit zwei Unterklassen, nicht static --> 2700


//    public String deepToString() {
//	return root.deepToString();
//    }



    //TODO: Vielleicht sollten die Nodes den d-Parameter des Baumes verwenden,
    // also selber nicht statisch sein (dasselbe gilt für die capacity)




    private class InternalNode extends Node {
	private final Node[] children;
	final float[] mbrsChildrenLow;
	final float[] mbrsChildrenHigh;

	private InternalNode() {
	    children = new Node[nodeCapacityMax];
	    mbrsChildrenLow = new float[nodeCapacityMax * d];
	    mbrsChildrenHigh = new float[nodeCapacityMax * d];
	}

	@Override
	boolean isLeaf() {
	    return false;
	}
    }

    private class LeafNode extends Node {
	private final float[] points;

	private LeafNode() {
	    points = new float[nodeCapacityMax * d];
	}

	@Override
	boolean isLeaf() {
	    return true;
	}

	private void append(float[] point) {
	    System.arraycopy(point, 0, points, size * d, d);
	    size++;
	}
    }


    // Node should exhibit memory locality, i.e. it should be stored
    // in a single memory page, which usually is 4096 bytes long
//    private static class Node {
    private abstract class Node {
//	private static int TRAVERSE_INDENT = 4;

	int size = 0;
//	private final int d;
//	private final boolean isLeaf;
	final float[] mbrLow = new float[d];
	final float[] mbrHigh = new float[d];

	// Used by internal nodes only
//	private final Node[] children;
//	private final float[] mbrsChildrenLow;
//	private final float[] mbrsChildrenHigh;

	// Used by leaf nodes only
//	private final float[] points;

	// creates an empty node
//	private Node(int d, boolean isLeaf, int nodeCapacityMax) {
//	private Node(boolean isLeaf) {
//	    this.d = d;
//	    size = 0;
//	    this.isLeaf = isLeaf;
//	    mbrLow = new float[d];
//	    mbrHigh = new float[d];
//	    if (!isLeaf) {
//		// internal node
//		children = new Node[nodeCapacityMax];
//		mbrsChildrenLow = new float[nodeCapacityMax * d];
//		mbrsChildrenHigh = new float[nodeCapacityMax * d];
//		points = null;
//	    } else {
//		// leaf node
//		children = null;
//		mbrsChildrenLow = null;
//		mbrsChildrenHigh = null;
//		points = new float[nodeCapacityMax * d];
//	    }
//	}
//
//	private void append(float[] point) {
//	    System.arraycopy(point, 0, points, size * d, d);
//	    size++;
//	}

	abstract boolean isLeaf();
//	private boolean isLeaf() {
//	    return isLeaf;
//	}

//	@Override
//	public String toString() {
//	    if (isLeaf) {
//		return String.format("%s: %s  (Size: %d)        RZ: %s / %s        Points: %s",
//			Integer.toHexString(hashCode()),
//			Arrays.toString(divideArray(Arrays.copyOfRange(keys, 0, size), STRING_SHORTENER)),
//			size,
//			Arrays.toString(rzBoundLow),
//			Arrays.toString(rzBoundHigh),
//			Arrays.toString(Arrays.copyOfRange(points, 0, size * d)));
//	    } else {
//		return String.format("%s: %s / %s  (Size: %d)        RZ_low: %s   RZ_high: %s        Bounds: %s / %s",
//			Integer.toHexString(hashCode()),
//			Arrays.toString(divideArray(Arrays.copyOfRange(keysChildrenLow, 0, size), STRING_SHORTENER)),
//			Arrays.toString(divideArray(Arrays.copyOfRange(keysChildrenHigh, 0, size), STRING_SHORTENER)),
//			size,
//			Arrays.toString(Arrays.copyOfRange(rzBoundsChildrenLow, 0, size * d)),
//			Arrays.toString(Arrays.copyOfRange(rzBoundsChildrenHigh, 0, size * d)),
//			Arrays.toString(rzBoundLow),
//			Arrays.toString(rzBoundHigh));
//	    }
//	}
//
//	private static long[] divideArray(long[] a, long d) {
//	    int n = a.length;
//	    long[] b = new long[n];
//	    for (int i = 0; i < n; i++) {
//		b[i] = a[i] / d;
//	    }
//	    return b;
//	}
//
//	public String deepToString() {
//	    return traverse(0).toString();
//	}
//
//	private StringBuffer traverse(int level) {
//	    StringBuffer out = new StringBuffer();
//	    out.append(String.format("%s%s\n", StringUtils.repeat(' ', level), this));
//	    if (!isLeaf) {
//		for (int i = 0; i < size; i++) {
//		    Node child = children[i];
//		    out.append(child.traverse(level + TRAVERSE_INDENT));
//		}
//	    }
//	    return out;
//	}
    }
}
