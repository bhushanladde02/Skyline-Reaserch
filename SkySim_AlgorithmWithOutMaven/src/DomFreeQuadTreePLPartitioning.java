

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DomFreeQuadTreePLPartitioning implements Iterable<float[]> {

    private Node root = null;
    private final int d;
    private final long onesMask;
    private long numOps = 0;
    private long numberOfNodesVisited = 0;
//    private final boolean presorted;

    private static final int INITIAL_ARRAY_SIZE = 20;


    // NOTE: Currently this algorithm cannot exploit presorted data.

    public DomFreeQuadTreePLPartitioning(PointSource ds, boolean presorted) {
//	this.presorted = presorted;
	if (presorted) {
	    throw new UnsupportedOperationException("Not supported yet.");
	}
	d = ds.getD();
	onesMask = (1l << d) - 1;
	Node node = new Node(0, null, null, null, ds.toFlatArray(), ds.size());
	root = processBucketNode(node);
    }


    private Node createInternalNodeFromBucketNode(Node n) {
	// Find some skyline point r in n.points,
	// while cleaning some dominated points on-the-fly.
	float[] r;
//	if (presorted) {
//	    r = Arrays.copyOf(n.points, d);
//	} else {
	    r = _findUndominated(n);
//	}

	Node nNew = new Node(r, null, n.nextSibling, n.parent);

	// Sort array according to pa, then insert into nNew within a single run.
	//
	// CONSTRAINT: The order of the points must stay the same within each bucket
	// since the data might have been presorted.
	//
	// However, our sort implementation does not support this property.
	// Therefore, currently this algorithm cannot exploit presorted data.
	int nSize = n.size;
	long[] sortvals = new long[nSize];
	for (int i = n.size - 1, pos = d * (nSize - 1); i >= 0; i--, pos -= d) {
	    float[] p = Arrays.copyOfRange(n.points, pos, pos + d);
	    sortvals[i] = PointComparator.getRegionIDOfBRelativeToAZhang(r, p, d);
	    numOps++;
	}
	ArraySorter.longArraySort(n.points, d, sortvals);

	int i = 0;
	int pos = 0;
	long paP = sortvals[i];
	if (paP != onesMask) {
	    // nNew has at least one child.
	    Node leaf = null;
	    long paPBlockBegin = paP;
	    int posBlockBegin = pos;
	    i++;
	    pos += d;
	    while (i < nSize) {
		paP = sortvals[i];
		if (paP == onesMask) {
		    break;
		} else if (paP != paPBlockBegin) {
		    // p is not dominated, a new block has begun.
		    // Save last block.
		    Node newLeaf = new Node(paPBlockBegin, null, null, nNew, n.points, posBlockBegin, pos);
		    numberOfNodesVisited++;
		    if (leaf == null) {
			nNew.firstSon = newLeaf;
		    } else {
			leaf.nextSibling = newLeaf;
		    }
		    leaf = newLeaf;
		    paPBlockBegin = paP;
		    posBlockBegin = pos;
		}
		i++;
		pos += d;
	    }
	    // Save last block
	    Node newLeaf = new Node(paPBlockBegin, null, null, nNew, n.points, posBlockBegin, pos);
	    numberOfNodesVisited++;
	    if (leaf == null) {
		nNew.firstSon = newLeaf;
	    } else {
		leaf.nextSibling = newLeaf;
	    }
	}

	return nNew;
    }

    private Node processBucketNode(Node n) {
	numberOfNodesVisited++;
	Node nNew;  // this node will be returned (and will replace n)
	if (n.size == 1) {
	    nNew = new Node(Arrays.copyOf(n.points, d), null, n.nextSibling, n.parent);
	} else {
	    // Create a new internal node nNew (indented to replace n) with some skyline point r as split point,
	    // insert all remaining points (not being dominated by r)
	    // into new leaf nodes just below r's new node.
	    nNew = createInternalNodeFromBucketNode(n);

	    // Process nNew's children.
	    Node son = nNew.firstSon;
	    if (son != null) {
		Node newSon = processBucketNode(son);
		nNew.firstSon = newSon;
		Node prev = newSon;
		son = prev.nextSibling;
		while (son != null) {
		    newSon = processBucketNode(son);
		    prev.nextSibling = newSon;
		    prev = newSon;
		    son = prev.nextSibling;
		}
	    }
	}
	// Now, all children of nNew have been replaced by regular nodes.

	// Scan over all remaining siblings of nNew and remove
	// all points in these leaf nodes that are dominated by some node in nNew's tree
	// (using the bit criteron for selecting the leaf nodes to be checked).
	// Delete all empty leaves.
	long paR = nNew.pa;
	Node prevSibling = nNew;
	Node sibling = nNew.nextSibling;
	while (sibling != null) {
	    numberOfNodesVisited++;
	    long paSibling = sibling.pa;
	    if ((paSibling | paR) == paSibling) {
		// There might be points in sibling.points that
		// are dominated by some node in nNew's tree;
		// let's check and delete them.
		int dSize = d * sibling.size;
		for (int pos = dSize - d; pos >= 0; pos -= d) {
		    int posD = pos + d;
		    float[] p = Arrays.copyOfRange(sibling.points, pos, posD);
		    if (_isDominated(nNew, p)) {
			System.arraycopy(sibling.points, posD, sibling.points, pos, dSize - posD);
			sibling.size--;
			dSize -= d;
		    }
		}
	    }
	    if (sibling.size == 0) {
		// Delete sibling.
		prevSibling.nextSibling = sibling.nextSibling;
		sibling = sibling.nextSibling;
	    } else {
		prevSibling = sibling;
		sibling = sibling.nextSibling;
	    }
	}

	return nNew;
    }

    private boolean _isDominated(Node e, float[] o) {
        // e (and its children) could possibly dominate o.
	long Pa = PointComparator.getRegionIDOfBRelativeToAZhang(e.point, o, d);
	numOps++;
	if (Pa == onesMask) {
	    // e dominates o.
	    return true;
	}
	// e does not dominate o. Let's check e's children.
	if ((e.firstSon != null) && (e.firstSon.pa <= Pa)) {
	    // The first son of e possibly dominates o.
	    Node son = e.firstSon;
	    do {
		numberOfNodesVisited++;
		if (((son.pa | Pa) == Pa) && (_isDominated(son, o))) {
		    return true;
		}
		son = son.nextSibling;
	    } while ((son != null) && (son.pa <= Pa));
	}
	return false;
    }


    // Finds some skyline point in a list of @d-dimensional points,
    // which is stored as an array @data, containing @m points.
    private float[] _findUndominated(Node n) {
	// equality handling not supported yet
	int m = n.size;
	float[] data = n.points;
	int md = m * d;
	float[] candidate = Arrays.copyOfRange(data, md - d, md);
	dataloop:
	for (int posDataStart = md - d - 1; posDataStart >= 0; posDataStart -= d) {
	    numOps++;
	    int posData = posDataStart;
	    int posCand = d - 1;
	    if (candidate[posCand] >= data[posData]) {
		while (posCand > 0) {
		    posCand--;
		    posData--;
		    if (candidate[posCand] < data[posData]) {
			continue dataloop; // incomparable
		    }
		}
		// @cand dominates
	    } else {
		while (posCand > 0) {
		    posCand--;
		    posData--;
		    if (candidate[posCand] > data[posData]) {
			continue dataloop; // incomparable
		    }
		}
		candidate = Arrays.copyOfRange(data, posDataStart - (d - 1), posDataStart + 1); // @cand is dominated
	    }
	}
	return candidate;
    }


    public long getNumberOfComparions() {
	return numOps;
    }

     public long getNumberOfNodesVisited() {
	return numberOfNodesVisited;
    }

    @Override
    public Iterator<float[]> iterator() {
	return new TreeIterator();
    }

    public String deepToString() {
	return root.deepToString();
    }

    public String getStats() {
	StringBuffer result = new StringBuffer();

	if (root != null) {
	    // Count the number of sons of the root.
	    int sonsOfRoot = 0;
	    Node son = root.firstSon;
	    while (son != null) {
		sonsOfRoot++;
		son = son.nextSibling;
	    }
	    result.append(String.format("The root node has %d sons.%n", sonsOfRoot));

	    // How "central" is the root node?
	    double centrality = 0;
	    for (int i = 0; i < d; i++) {
		centrality += 4 * root.point[i] * (1 - root.point[i]);
	    }
	    centrality /= d;
	    result.append(String.format("Its mean centrality is %.4f (1 means optimal centrality).", centrality));
	}

	return result.toString();
    }



    private class Node {
	private final boolean isBucket;
	private final float[] point;
	private final long pa;
	private Node nextSibling;
	private Node firstSon;
	private Node parent;
	private float[] points;
	private int size;

	private Node(float[] point, Node firstSon, Node nextSibling, Node parent) {
	    this(point,
		parent == null ? 0 : PointComparator.getRegionIDOfBRelativeToAZhang(parent.point, point, point.length),
		firstSon, nextSibling, parent);
	}

	private Node(float[] point, long pa, Node firstSon, Node nextSibling, Node parent) {
	    isBucket = false;
	    points = null;
	    this.point = Arrays.copyOf(point, point.length);
	    this.pa = pa;
	    this.firstSon = firstSon;
	    this.nextSibling = nextSibling;
	    this.parent = parent;
	}

	private Node(long pa, Node firstSon, Node nextSibling, Node parent, float[] points, int size) {
	    isBucket = true;
	    this.points = Arrays.copyOf(points, points.length);
	    this.size = size;
	    point = null;
	    this.pa = pa;
	    this.firstSon = firstSon;
	    this.nextSibling = nextSibling;
	    this.parent = parent;
	}

	private Node(long pa, Node firstSon, Node nextSibling, Node parent, float[] points, int posFrom, int posTo) {
	    isBucket = true;
	    this.points = Arrays.copyOfRange(points, posFrom, posTo);
	    size = (posTo - posFrom) / d;
	    point = null;
	    this.pa = pa;
	    this.firstSon = firstSon;
	    this.nextSibling = nextSibling;
	    this.parent = parent;
	}

	private Node(long pa, Node firstSon, Node nextSibling, Node parent) {
	    this(pa, firstSon, nextSibling, parent, new float[d * INITIAL_ARRAY_SIZE], 0);
	}

	private List<float[]> deepGetPoints() {
	    List<float[]> list = new LinkedPointList(d);
	    if (isBucket) {
		list.addAll(PointListTools.arrayToPointList(points, size, d));
	    } else {
		list.add(point);
		Node son = firstSon;
		while (son != null) {
		    list.addAll(son.deepGetPoints());
		    son = son.nextSibling;
		}
	    }
	    return list;
	}

	private String deepToString() {
	    return _deepToString(0);
	}

	private String _deepToString(int indent) {
	    StringBuffer result = new StringBuffer();
	    String indentStr = StringUtils.repeat(' ', indent);
	    result.append(indentStr + NumberUtils.arrayToString(point, 3) + " (" + pa + ")" + "\n");
	    Node son = firstSon;
	    while (son != null) {
		result.append(son._deepToString(indent + 2));
		son = son.nextSibling;
	    }
	    return result.toString();
	}
    }



    private class TreeIterator implements Iterator<float[]> {

	private Iterator<float[]> iter;

	private TreeIterator() {
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
}
