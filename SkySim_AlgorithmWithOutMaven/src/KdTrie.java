

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class KdTrie implements DomFreeTree {
    // CONFIG: Number of key bits handled by each level of the tree
    private static final int BITS_PER_LEVEL = 3;
    // CONFIG: Bucket capacity
    private static final int BUCKET_CAPACITY = 20;
    // CONFIG: Initial bucket size
    private static final int INITIAL_BUCKET_SIZE = BUCKET_CAPACITY;
    // CONFIG: Maximum number of bits used
    private static final int MAX_ZLENGTH = 256;

    // DERIVED: Split degree of each node (= 2^BITS_PER_LEVEL)
    private static final int SPLIT_DEGREE = 1 << BITS_PER_LEVEL;
    // DERIVED
    private static final int CHILD_ID_MASK = SPLIT_DEGREE - 1;
    // DERIVED
    private static final int LONGS_PER_Z_ADDRESS = NumberUtils.DIV_ROUND_UP(MAX_ZLENGTH, Long.SIZE);
    // DERIVED: Depth at which z address longs have to be refilled with new address parts
    private static final int REFILL_DEPTH = BITS_PER_LEVEL * (Long.SIZE / BITS_PER_LEVEL);
    /*
     * Z addresses will be refilled if needed just before a shift operation takes place,
     * i.e. temporarily, there might be "empty" z addresses stored in the tree.
     */

    private final int d;
    private final long dOnesMask;
    private Node root;
    private long numOps = 0;
    private long numberOfNodesVisited = 0;


    public KdTrie(int d) {
	if (d > Long.SIZE - BITS_PER_LEVEL + 1) { // because of activeDimsDouble
	    throw new IllegalArgumentException("Too many dimensions");
	}
	this.d = d;
	dOnesMask = (1l << d) - 1;
	root = null;
    }

    private void generateZAddressesAndPresortData(long[] zas, float[] data, int n) {
	if (LONGS_PER_Z_ADDRESS == 1) {
	    for (int i = 0, posStart = 0; i < n; i++, posStart += d) {
		zas[i] = NumberUtils.unsignedZAddress(data, posStart, d);
		numOps++;
	    }
	    ArraySorter.unsignedLongArraySortDecreasing(data, d, zas);
	} else {
	    long[] za = new long[n * LONGS_PER_Z_ADDRESS];
	    for (int i = 0, posZ = 0, posStart = 0; i < n; i++, posZ += LONGS_PER_Z_ADDRESS, posStart += d) {
		long[] z = NumberUtils.unsignedZAddress(data, posStart, d, MAX_ZLENGTH);
		System.arraycopy(z, 0, za, posZ, LONGS_PER_Z_ADDRESS);
		numOps++;
	    }
	    ArraySorter.unsignedLongArraySortDecreasing(data, d, za, LONGS_PER_Z_ADDRESS);
	    for (int i = 0, posZ = 0; i < n; i++, posZ += LONGS_PER_Z_ADDRESS) {
		zas[i] = za[posZ];
	    }
	}
    }

    // Bulkload by Z presorting
    public KdTrie(PointSource ds) {
	this(ds.getD());
	float[] data = ds.toFlatArray();

	// Generate z addresses and presort points
	int n = ds.size();
	long[] zas = new long[n];
	generateZAddressesAndPresortData(zas, data, n);

	// Insert points
	for (int i = 0, pos = 0; i < n; i++, pos += d) {
	    float[] p = Arrays.copyOfRange(data, pos, pos + d);
	    simpleInsertIfUndominated(p, zas[i]);
	}
    }



    // Bulkload by partitioning
    public KdTrie(PointSource ds, boolean presorted) {
	this(ds.getD());
	float[] data = ds.toFlatArray();

	// Generate z addresses, sort by z address
	int n = ds.size();
	long[] zas = new long[n];
	generateZAddressesAndPresortData(zas, data, n);
	for (int i = 0; i < n; i++) {
	    zas[i] = Long.reverse(zas[i]);
	}

	// Start partitioning process
	Node node = new Node(data, zas, null, -1);
	root = processBucketNode(node, 0, -1, 0);
    }

    private Node processBucketNode(Node n, int currentDim, int previousDim, int depth) {
	// CurrentDim is set according to how many dims have been
	// removed from the z addresses of n's points yet.
	//
	// We know that all of n's points share the same z address prefix
	// (which has been removed already).
	numberOfNodesVisited++;
	Node nNew;  // this node will be returned (and will replace n)
	int size = n.size;
	if ((size <= BUCKET_CAPACITY) || (depth >= MAX_ZLENGTH)) {
	    // Although these nodes haven't been dominated yet, they might dominate each other ...
	    n.size = SkylineAlgorithmSskyline.performSskylineRun(n.points, d, 0, d * size);
	    // We don't need to keep track of n.point's order or n.za since
	    // this is a bucket node we will never extend again.
	    nNew = n;
	} else {
	    if ((LONGS_PER_Z_ADDRESS > 1) && (depth != 0) && (depth % REFILL_DEPTH == 0)) {
		// Refill z addresses.
		for (int i = 0, pos = 0; i < n.size; i++, pos += d) {
		    n.zas[i] = Long.reverse(NumberUtils.unsignedZAddressStartingAt(n.points, pos, d, depth));
		}
	    }

	    // Create a new internal node nNew (indented to replace n),
	    // insert all remaining points into new leaf nodes just below nNew.
	    nNew = new Node(false, n.parent, n.cid);
	    int cidOfCurrentBlock = (int)(n.zas[0] & CHILD_ID_MASK);
	    n.zas[0] >>>= BITS_PER_LEVEL;
	    int startPosOfCurrentBlock = 0;
	    int startIOfCurrentBlock = 0;

	    for (int i = 1, pos = d; i < size; i++, pos += d) {
		long zaPIrem = n.zas[i];
		int cid = (int)(zaPIrem & CHILD_ID_MASK);
		n.zas[i] >>>= BITS_PER_LEVEL;
		if (cidOfCurrentBlock != cid) {
		    // A new block has begun; save the previous block.
		    nNew.children[cidOfCurrentBlock] = new Node(n.points, startPosOfCurrentBlock, pos, n.zas, startIOfCurrentBlock, i, nNew, cidOfCurrentBlock);
		    cidOfCurrentBlock = cid;
		    startPosOfCurrentBlock = pos;
		    startIOfCurrentBlock = i;
		}
	    }
	    // Save the last block
	    nNew.children[cidOfCurrentBlock] = new Node(n.points, startPosOfCurrentBlock, d * size, n.zas, startIOfCurrentBlock, size, nNew, cidOfCurrentBlock);

	    // Process nNew's children.
	    int nextDim = NumberUtils.fastCyclicIncrement(currentDim, BITS_PER_LEVEL, d);
	    for (int i = CHILD_ID_MASK; i >= 0; i--) {
		Node son = nNew.children[i];
		if (son != null) {
		    nNew.children[i] = processBucketNode(son, nextDim, currentDim, depth + BITS_PER_LEVEL);
		}
	    }
	}
	// Now, all children of nNew have been replaced by regular nodes.

	// Scan over all remaining siblings of nNew and remove
	// all points in these leaf nodes that are dominated by some node in nNew's tree
	// (using the bit criteron for selecting the leaf nodes to be checked).
	// Delete all empty leaves.
	Node parent = nNew.parent;
	if (parent != null) {
	    int nNewCid = nNew.cid;
	    for (int i = nNewCid - 1; i >= 0; i--) {
		Node sibling = parent.children[i];
		if (sibling != null) {
		    numberOfNodesVisited++;
		    if ((i | nNewCid) == nNewCid) {
			// Create mask of active dims.
			// Before this step all dims have been active since
			// all points to be compared share the same prefix.
			long disabledDims = ((long)(~i & nNewCid) << previousDim);
			disabledDims |= (disabledDims >>> d); // handle overlapping dims
			long activeDims = dOnesMask & ~disabledDims;

			// There might be points in sibling.points that
			// are dominated by some node in nNew's tree;
			// let's check and delete them.
			int sizeSib = sibling.size;
			int dSizeSib = d * sizeSib;
			for (int j = sibling.size - 1, pos = dSizeSib - d; j >= 0; j--, pos -= d) {
			    int posD = pos + d;
			    float[] p = Arrays.copyOfRange(sibling.points, pos, posD);
			    long zaPrem = sibling.zas[j];
			    if (_isDominated(p, zaPrem, nNew, activeDims, currentDim, depth)) {
				System.arraycopy(sibling.points, posD, sibling.points, pos, dSizeSib - posD);
				System.arraycopy(sibling.zas, j + 1, sibling.zas, j, sizeSib - (j + 1));
				dSizeSib -= d;
				sizeSib--;
			    }
			}
			sibling.size = sizeSib;
		    }
		    if (sibling.size == 0) {
			// Delete sibling.
			parent.children[i] = null;
		    }
		}
	    }
	}

	return nNew;
    }



    @Override
    public boolean isDominated(float[] p) {
	long zaP = Long.reverse(NumberUtils.unsignedZAddress(p));
	return isDominated(p, zaP);
    }

    private boolean isDominated(float[] p, long zaP) {
	if (root == null) {
	    return false;
	} else {
	    return _isDominated(p, zaP, root, dOnesMask, 0, 0);
	}
    }

    private boolean _isDominated(float[] p, long zaPrem, Node node, long activeDims, int currentDim, int depth) {
	numberOfNodesVisited++;
	if (node.isLeaf) {
	    return PointComparator.isDominated(p, node.points, node.size, d);
	} else {
	    // @node is an internal node
	    if ((LONGS_PER_Z_ADDRESS > 1) && (depth != 0) && (depth % REFILL_DEPTH == 0)) {
		// Refill z address.
		zaPrem = Long.reverse(NumberUtils.unsignedZAddressStartingAt(p, depth));
	    }
	    int pCID = (int)(zaPrem & CHILD_ID_MASK);
	    long activeDimsDoubled = activeDims | (activeDims << d);
	    int activeDimsMask = (int)(activeDimsDoubled >>> currentDim) & CHILD_ID_MASK;
	    int mask = pCID & activeDimsMask; // set the dimensions of pCID to zero in which all children already dominate p
	    int nextDim = NumberUtils.fastCyclicIncrement(currentDim, BITS_PER_LEVEL, d);
	    for (int scanCID = SPLIT_DEGREE - 1; scanCID >= mask; scanCID--) {
		Node child = node.children[scanCID];
		if (((scanCID & mask) == mask) && (child != null)) { // the order of conditions does not matter wrt. performance
		    numberOfNodesVisited++;
		    long disableDims = (long)(~pCID & scanCID) << currentDim;
		    disableDims |= (disableDims >>> d); // handle overlapping dims
		    long activeDimsChild = activeDims & ~disableDims;
		    if (_isDominated(p, zaPrem >>> BITS_PER_LEVEL, child, activeDimsChild, nextDim, depth + BITS_PER_LEVEL)) {
			return true;
		    }
		}
	    }
	    return false;
	}
    }

    @Override
    public void simpleInsertIfUndominated(float[] p) {
	long zaPOrg = NumberUtils.unsignedZAddress(p);
	simpleInsertIfUndominated(p, zaPOrg);
    }

    public void simpleInsertIfUndominated(float[] p, long zaPOrg) {
	long zaP = Long.reverse(zaPOrg);
	if (!isDominated(p, zaP)) {
	    insert(p, zaP);
	}
    }

    @Override
    public void dominationFreeInsertIfUndominated(float[] p) {
	long zaPOrg = NumberUtils.unsignedZAddress(p);
	dominationFreeInsertIfUndominated(p, zaPOrg);
    }

    private void dominationFreeInsertIfUndominated(float[] p, long zaPOrg) {
	long zaP = Long.reverse(zaPOrg);
	if (!isDominated(p, zaP)) {
	    deleteDominated(p, zaP);
	    insert(p, zaP);
	}
    }

    private void insert(float[] p, long zaPrem) {
	if (root == null) {
	    root = new Node(p, zaPrem, null, -1);
	    numberOfNodesVisited++;
	} else {
	    int depth = 0;
	    Node node = root;
	    // Find/create a suitable leaf node and insert the point.
	    while (true) {
		numberOfNodesVisited++;
		if (node.isLeaf) {
		    // CASE 1: @node is a leaf node
		    if ((node.size < BUCKET_CAPACITY) || (depth >= MAX_ZLENGTH)) {
			// CASE 1a: append @p to @node
			node.addToLeafNode(p, zaPrem);
			return;
		    } else {
			// CASE 1b: split @node (i.e. replace it by an internal node)
			Node parent = node.parent;
			int childIdOfNode = node.cid;
			Node nNew = new Node(false, parent, childIdOfNode);
			if (parent == null) {
			    root = nNew;
			} else {
			    parent.children[childIdOfNode] = nNew;
			}
			int nodeSize = node.size;
			boolean refillZAddresses = false;
			if (LONGS_PER_Z_ADDRESS > 1) {
			    refillZAddresses = (depth % REFILL_DEPTH == 0);
			}
			for (int i = 0, pos = 0; i < nodeSize; i++, pos += d) {
			    numberOfNodesVisited++;
			    float[] pI = Arrays.copyOfRange(node.points, pos, pos + d);
			    long zaPIrem;
			    if ((LONGS_PER_Z_ADDRESS > 1) && refillZAddresses) {
				zaPIrem = Long.reverse(NumberUtils.unsignedZAddressStartingAt(pI, depth));
			    } else {
				zaPIrem = node.zas[i];
			    }
			    int childIdOfPI = (int)(zaPIrem & CHILD_ID_MASK);
			    if (nNew.children[childIdOfPI] == null) {
				nNew.children[childIdOfPI] = new Node(pI, zaPIrem >>> BITS_PER_LEVEL, nNew, childIdOfPI);
			    } else {
				nNew.children[childIdOfPI].addToLeafNode(pI, zaPIrem >>> BITS_PER_LEVEL);
			    }
			}
			node = nNew;
		    }
		} else {
		    // CASE 2: @node is an internal node --> step down a level
		    if ((LONGS_PER_Z_ADDRESS > 1) && (depth != 0) && (depth % REFILL_DEPTH == 0)) {
			// Refill z address.
			zaPrem = Long.reverse(NumberUtils.unsignedZAddressStartingAt(p, depth));
		    }
		    int childIdOfNode = (int)(zaPrem & CHILD_ID_MASK);
		    zaPrem >>>= BITS_PER_LEVEL;
		    depth += BITS_PER_LEVEL;
		    Node nextNode = node.children[childIdOfNode];
		    if (nextNode == null) {
			// A new leaf node has to be created
			Node lNode = new Node(p, zaPrem, node, childIdOfNode);
			numberOfNodesVisited++;
			node.children[childIdOfNode] = lNode;
			return;
		    } else {
			node = nextNode;
		    }
		}
	    }
	}
    }


    private void deleteDominated(float[] p, long zaP) {
	if (root != null) {
	    _deleteDominated(p, zaP, root, dOnesMask, 0, 0);
	}
    }

    private void _deleteDominated(float[] p, long zaPrem, Node node, long activeDims, int currentDim, int depth) {
	numberOfNodesVisited++;
	if (node.isLeaf) {
	    int dSize = d * node.size;
	    for (int i = node.size - 1, pos = dSize - d; i >= 0; i--, pos -= d) {
		numOps++;
		if (PointComparator.compare(node.points, pos, p, d) == PointRelationship.IS_DOMINATED_BY) {
		    System.arraycopy(node.points, pos + d, node.points, pos, dSize - (pos + d));
		    System.arraycopy(node.zas, i + 1, node.zas, i, node.size - (i + 1));
		    node.size--;
		    dSize -= d;
		}
		// Unlink empty leaf nodes
		if (node.size == 0) {
		    Node parent = node.parent;
		    if (parent == null) {
			root = null;
		    } else {
			int cid = node.cid;
			parent.children[cid] = null;
			numberOfNodesVisited++;
		    }
		}
	    }
	} else {
	    // Refill z address.
	    if ((LONGS_PER_Z_ADDRESS > 1) && (depth != 0) && (depth % REFILL_DEPTH == 0)) {
		zaPrem = Long.reverse(NumberUtils.unsignedZAddressStartingAt(p, depth));
	    }

	    // @node is an internal node
	    int pCID = (int)(zaPrem & CHILD_ID_MASK);
	    long activeDimsDoubled = activeDims | (activeDims << d);
	    int inactiveDimsMask = ~(int)(activeDimsDoubled >>> currentDim) & CHILD_ID_MASK;
	    int mask = pCID | inactiveDimsMask; // set the dimensions of pCID to 1 in which all children are already dominated by p

	    long zaPremChild = zaPrem >>> BITS_PER_LEVEL;
	    int nextDim = NumberUtils.fastCyclicIncrement(currentDim, BITS_PER_LEVEL, d);

	    for (int scanCID = mask; scanCID >= 0; scanCID--) {
		Node child = node.children[scanCID];
		if (((scanCID & mask) == scanCID) && (child != null)) { // the order of conditions does not matter wrt. performance (???)
		    long disableDims = (pCID & ~scanCID) << currentDim;
		    disableDims |= (disableDims >>> d); // handle overlapping dims
		    long activeDimsChild = activeDims & ~disableDims;
		    _deleteDominated(p, zaPremChild, child, activeDimsChild, nextDim, depth + BITS_PER_LEVEL);
		}
	    }
	}
    }


    @Override
    public long getNumberOfComparions() {
	return numOps;
    }

    @Override
    public long getNumberOfNodesVisited() {
	return numberOfNodesVisited;
    }

    @Override
    public Iterator<float[]> iterator() {
	return new TreeIterator();
    }

    @Override
    public String deepToString() {
	if (root == null) {
	    return "null";
	} else {
	    return root.deepToString();
	}
    }

    @Override
    public String getStats() {
	StringBuffer result = new StringBuffer();
	StatCounter sc = new StatCounter();
	if (root != null) {
	    root.getStats(sc, 0);
	}
	result.append(String.format("#internal nodes: %d%n", sc.internalNodes));
	result.append(String.format("#internal bucket nodes: %d%n", sc.bucketNodesInternal));
	result.append(String.format("average size of an internal bucket node: %.1f%n", (double)sc.sizeBucketNodesInternal / sc.bucketNodesInternal));
	result.append(String.format("#leaf bucket nodes: %d%n", sc.bucketNodesMaxDepth));
	result.append(String.format("average size of a leaf bucket node: %.1f%n", (double)sc.sizeBucketNodesMaxDepth / sc.bucketNodesMaxDepth));
	result.append(String.format("maximum depth: %d (= %d levels)", sc.maxDepth, sc.maxDepth / BITS_PER_LEVEL + 1));
	return result.toString();
    }

    @Override
    public void addToNumberOfComparisons(long num) {
	numOps += num;
    }

    private class Node { // Making Node static does not provide any performance gain.
	private final boolean isLeaf;
	private int size;
	private final Node[] children;
	private float[] points; // replacing this by a PointList slows this implementation down by a factor of 2.
	private long[] zas;
	private Node parent;
	private int cid;

	private Node(boolean isLeaf, Node parent, int cid) {
	    this.isLeaf = isLeaf;
	    this.parent = parent;
	    this.cid = cid;
	    if (isLeaf) {
		size = 0;
		children = null;
		points = new float[INITIAL_BUCKET_SIZE * d];
		zas = new long[INITIAL_BUCKET_SIZE];
	    } else {
		size = -1;
		children = new Node[SPLIT_DEGREE];
		points = null;
		zas = null;
	    }
	}

	private Node(float[] p, long zaPrem, Node parent, int cid) {
	    this(true, parent, cid);
	    addToLeafNode(p, zaPrem);
	}

	private Node(float[] points, int pointsFrom, int pointsTo, long[] zas, int zasFrom, int zasTo, Node parent, int cid) {
	    isLeaf = true;
	    this.parent = parent;
	    this.cid = cid;
	    size = zasTo - zasFrom;
	    children = null;
	    this.points = Arrays.copyOfRange(points, pointsFrom, pointsTo);
	    this.zas= Arrays.copyOfRange(zas, zasFrom, zasTo);
	}

	private Node(float[] data, long[] zaPrems, Node parent, int cid) {
	    isLeaf = true;
	    this.parent = parent;
	    this.cid = cid;
	    size = zaPrems.length;
	    children = null;
	    points = Arrays.copyOf(data, data.length);
	    zas = Arrays.copyOf(zaPrems, size);
	}

	// Adds @p to a leaf node (without doing any node splitting).
	// @zaPrem: remaining part of @p's z address
	private void addToLeafNode(float[] p, long zaPrem) {
	    addToLeafNode(p, 0, zaPrem);
	}

	private void addToLeafNode(float[] data, int posStart, long zaPrem) {
	    int sizeD = size * d;
	    if (sizeD == points.length) {
		// Extend arrays.
		points = Arrays.copyOf(points, 2 * points.length);
		zas = Arrays.copyOf(zas, 2 * zas.length);
	    }
	    // Append point
	    System.arraycopy(data, posStart, points, sizeD, d);
	    zas[size] = zaPrem;
	    size++;
	}

	private static final int FLOAT_NUM_PLACES = 4;
	private static final int NO_CID = -1;

	@Override
	public String toString() {
	    return "#" + Integer.toHexString(this.hashCode());
	}

	private void getStats(StatCounter sc, int depth) {
	    if (depth > sc.maxDepth) {
		sc.maxDepth = depth;
	    }
	    if (isLeaf) {
		if (depth >= MAX_ZLENGTH) {
		    sc.bucketNodesMaxDepth++;
		    sc.sizeBucketNodesMaxDepth += size;
		} else {
		    sc.bucketNodesInternal++;
		    sc.sizeBucketNodesInternal += size;
		}
	    } else {
		sc.internalNodes++;
		for (int i = 0; i < SPLIT_DEGREE; i++) {
		    Node child = children[i];
		    if (child != null) {
			child.getStats(sc, depth + BITS_PER_LEVEL);
		    }
		}
	    }
	}

	private List<float[]> deepGetPoints() {
	    List<float[]> list = new LinkedPointList(d);
	    if (isLeaf) {
		for (int i = 0; i < d * size; i += d) {
		    float[] p = Arrays.copyOfRange(points, i, i + d);
		    list.add(p);
		}
	    } else {
		for (int i = 0; i < SPLIT_DEGREE; i++) {
		    Node child = children[i];
		    if (child != null) {
			list.addAll(child.deepGetPoints());
		    }
		}
	    }
	    return list;
	}

	private String deepToString() {
	    return deepToString(NO_CID, 0);
	}

	private String deepToString(int cid, int depth) {
	    String type;
	    if (isLeaf) {
		type = "leaf";
	    } else {
		type = "internal";
	    }
	    String cidString;
	    if (cid == NO_CID) {
		cidString = "";
	    } else {
		cidString = NumberUtils.binaryString(Integer.reverse(cid), 0, BITS_PER_LEVEL);
	    }
	    String result = String.format("%s%s (%s, %s, size %d, depth %d)%n",
		    StringUtils.repeat(' ', depth - cidString.length()),
		    cidString,
		    type,
		    "", //this,
		    size,
		    depth);
	    if (isLeaf) {
		for (int i = 0; i < size; i++) {
		    float[] p = Arrays.copyOfRange(points, i * d, (i + 1) * d);
		    result += String.format("%s%s (%s)%n",
//			    StringUtils.repeat(' ', depth),
//			    NumberUtils.binaryString(NumberUtils.unsignedZAddress(p), depth, MAX_ZLENGTH - depth),
			    "",
			    NumberUtils.binaryString(NumberUtils.unsignedZAddress(p), 0, MAX_ZLENGTH),
			    NumberUtils.arrayToString(p, FLOAT_NUM_PLACES));
		}
	    } else {
		for (int i = 0; i < SPLIT_DEGREE; i++) {
		    if (children[i] != null) {
			result += children[i].deepToString(i, depth + BITS_PER_LEVEL);
		    }
		}
	    }
	    return result;
	}
    }

    private class TreeIterator implements Iterator<float[]> {

	private Iterator<float[]> iter;

	public TreeIterator() {
	    List<float[]> list;
	    if (root == null) {
		list = new LinkedPointList(d);
	    } else {
		list = root.deepGetPoints();
	    }
	    iter = list.iterator();
	}

	@Override
	public boolean hasNext() {
	    return iter.hasNext();
	}

	@Override
	public float[] next() {
	    return iter.next();
	}

	@Override
	public void remove() {
	    throw new UnsupportedOperationException("Not supported yet.");
	}

    }

    private class StatCounter {
	private int internalNodes = 0;
	private int bucketNodesInternal = 0;
	private int bucketNodesMaxDepth = 0;
	private int sizeBucketNodesInternal = 0;
	private int sizeBucketNodesMaxDepth = 0;
	private int maxDepth = -1;
    }
}