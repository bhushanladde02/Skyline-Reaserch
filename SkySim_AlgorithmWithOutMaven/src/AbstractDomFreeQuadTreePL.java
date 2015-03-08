

import java.util.Arrays;
import java.util.Iterator;

/*
 * A recursive implementation of Habenicht's domination-free quad trees,
 * using primogenitary linked quad trees (PLQT) as proposed in
 * Sun: A Priomogenitary Linked Quad Tree Data Structure and
 *      Its Application to Discrete Multiple Criteria Optimization
 *
 * In fact, this as fast as using a (growing) array instead of a linked
 * list of children. Internally, Java seems to store both data structures in the same way.
 */

public abstract class AbstractDomFreeQuadTreePL extends AbstractDomFreeTree {
    protected Node root;
    protected final int d;
    protected final long onesMask;
    protected long numComps = 0;
    protected long numberOfNodesVisited = 0;

    public AbstractDomFreeQuadTreePL(int d) {
	root = null;
	this.d = d;
	onesMask = (1l << d) - 1;
    }

    protected AbstractDomFreeQuadTreePL() {
	throw new UnsupportedOperationException();
    }

    @Override
    public void simpleInsertIfUndominated(float[] p) {
	if (root == null) {
	    insert(p);
	} else {
	    _isDominatedAndInsertIfUndominated(root, p, true);
	}
    }

    /*
     * Inserts @p into the tree, without doing any dominance checking.
     * A copy of @p will be created when the node is created.
     */
    @Override
    protected void insert(float[] p) {
	if (root == null) {
	    root = new Node(p, null, null, null);
	    numberOfNodesVisited++;
	} else {
	    _insert(root, p);
	}
    }

    protected void _insert(Node n, float[] p) {
	numberOfNodesVisited++;
	long pa = PointComparator.getRegionIDOfBRelativeToAZhang(n.point, p, d);
	numComps++;
	if ((n.firstSon == null) || (pa < n.firstSon.pa)) {
	    // Insert p as the first son of n.
	    n.firstSon = new Node(p, null, n.firstSon, n);
	    numberOfNodesVisited++;
	} else {
	    // Search for an insertion position matching p.
	    Node prev = null;
	    Node next = n.firstSon;
	    while ((next != null) && (next.pa < pa)) {
		numberOfNodesVisited++;
		prev = next;
		next = next.nextSibling;
	    }
	    if ((next != null) && (next.pa == pa)) {
		_insert(next, p);
	    } else {
		prev.nextSibling = new Node(p, null, next, n);
		numberOfNodesVisited++;
	    }
	}
    }


    /*
     * Checks if p is dominated by some point in the tree.
     */
    @Override
    public boolean isDominated(float[] p) {
	if (root == null) {
	    return false;
	} else {
	    return _isDominated(root, p);
	}
    }

    private boolean _isDominated(Node e, float[] o) {
	numberOfNodesVisited++;
        // e (and its children) could possibly dominate o.
	long Pa = PointComparator.getRegionIDOfBRelativeToAZhang(e.point, o, d);
	numComps++;
	if (Pa == onesMask) {
	    // e dominates o.
	    return true;
	}
	// e does not dominate o. Let's check e's children.
	Node son = e.firstSon;
	if ((son != null) && (son.pa <= Pa)) {
	    // The first son of e possibly dominates o;
	    // check it all all remaining sons.
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

    private boolean _isDominatedAndInsertIfUndominated(Node e, float[] o, boolean onTrack) {
	numberOfNodesVisited++;
        // e (and its children) could possibly dominate o.
	long Pa = PointComparator.getRegionIDOfBRelativeToAZhang(e.point, o, d);
	numComps++;
	if (Pa == onesMask) {
	    // e dominates o.
	    return true;
	}
	// e does not dominate o. Let's check e's children.
	Node prevSon = null;
	Node son = e.firstSon;
	if ((son != null) && (son.pa <= Pa)) {
	    // The first son of e possibly dominates o;
	    // check it all all remaining sons.
	    do {
		numberOfNodesVisited++;
		if ((son.pa | Pa) == Pa) {
		    boolean stillOnTrack = onTrack && (son.pa == Pa);
		    if (_isDominatedAndInsertIfUndominated(son, o, stillOnTrack)) {
			return true;
		    }
		}
		prevSon = son;
		son = son.nextSibling;
	    } while ((son != null) && (son.pa <= Pa));
	}
	// Check whether o must be inserted here.
	if (onTrack) {
	    if ((prevSon == null) || (prevSon.pa != Pa)) {
		// Insert o between prevSon and son.
		numberOfNodesVisited++;
		Node newNode = new Node(o, null, son, e);
		if (prevSon == null) {
		    e.firstSon = newNode;
		} else {
		    prevSon.nextSibling = newNode;
		}
	    }
	}

	return false;
    }

    @Override
    public Iterator<float[]> iterator() {
	return new TreeIterator(this);
    }

    @Override
    public long getNumberOfComparions() {
	return numComps;
    }

    @Override
    public void addToNumberOfComparisons(long num) {
	numComps += num;
    }

    @Override
    public long getNumberOfNodesVisited() {
	return numberOfNodesVisited;
    }

    @Override
    public String deepToString() {
	return root.toString();
    }

    @Override
    public String getStats() {
	// Count the number of sons of the root.
	int sonsOfRoot = 0;
	if (root != null) {
	    Node son = root.firstSon;
	    while (son != null) {
		sonsOfRoot++;
		son = son.nextSibling;
	    }
	}
	return String.format("The root node has %d sons.", sonsOfRoot);
    }





    protected static class Node {
	protected final float[] point;
	protected long pa;
	protected long pa2; // needed for finding dominated nodes
	protected Node nextSibling;
	protected Node firstSon;
	protected Node parent; // needed for toString and TreeIterator
	protected boolean deleted = false;

	private static final int INDENT = 4;

	public Node(float[] point, Node firstSon, Node nextSibling, Node parent) {
	    this(point,
		parent == null ? 0 : PointComparator.getRegionIDOfBRelativeToAZhang(parent.point, point, point.length),
		parent == null ? 0 : PointComparator.getRegionIDOfBRelativeToAZhang2(parent.point, point, point.length),
		firstSon, nextSibling, parent);
	}

	public Node(float[] point, long pa, long pa2, Node firstSon, Node nextSibling, Node parent) {
	    this.point = Arrays.copyOf(point, point.length);
	    this.pa = pa;
	    this.pa2 = pa2;
	    this.firstSon = firstSon;
	    this.nextSibling = nextSibling;
	    this.parent = parent;
	}

	@Override
	public String toString() {
	    StringBuffer out = new StringBuffer();
	    Node currentNode = this;
	    int currentLevel = 0;
	    do {
		String deletedStr = "";
		if (currentNode.deleted) {
		    deletedStr = " (deleted)";
		}
		out.append(String.format("%s%d/%d%s: %s\n",
			StringUtils.repeat(' ', currentLevel * INDENT),
			currentNode.pa,
			currentNode.pa2,
			deletedStr,
			Arrays.toString(currentNode.point)));
		if (currentNode.firstSon != null) {
		    currentNode = currentNode.firstSon;
		    currentLevel++;
		} else {
		    while (currentNode.nextSibling == null) {
			currentNode = currentNode.parent;
			currentLevel--;
			if (currentLevel < 0) {
			    return out.toString();
			}
		    }
		    currentNode = currentNode.nextSibling;
		}
	    } while (true);
	}
    }

    protected static class TreeIterator implements Iterator<float[]> {

	private Node nextNode;

	private TreeIterator(AbstractDomFreeQuadTreePL tree) {
	    if (tree.root.deleted) {
		nextNode = getNextUndeletedOf(tree.root);
	    } else {
		nextNode = tree.root;
	    }
	}

	@Override
	public boolean hasNext() {
	    return (nextNode != null);
	}

	@Override
	public float[] next() {
	    float[] result = nextNode.point;
	    nextNode = getNextUndeletedOf(nextNode);
	    return result;
	}

	protected static Node getNextOf(Node node) {
	    if (node.firstSon != null) {
		return node.firstSon;
	    } else {
		Node next = node;
		while (next.nextSibling == null) {
		    next = next.parent;
		    if (next == null) {
			return null;
		    }
		}
		next = next.nextSibling;
		return next;
	    }
	}

	private static Node getNextUndeletedOf(Node node) {
	    Node next = node;
	    do {
		next = getNextOf(next);
	    } while ((next != null) && (next.deleted));
	    return next;
	}

	@Override
	public void remove() {
	    throw new UnsupportedOperationException("Not supported yet.");
	}
    }
}
