

import java.util.Arrays;
import java.util.Iterator;

/*
 * Dominance decision tree as described in:
 * Oliver Schütze: A New Data Structure for the Nondominance Problem in Multi-Objective Optimization (2003)
 *
 * Although this implementation uses a lot of recursive calls,
 * we did not find an iterative implementation that performed better.
 */

public class DominanceDecisionTree extends AbstractDomFreeTree {

    private Node root;
    private long numComp = 0;
    private final int d;

    public DominanceDecisionTree(int d) {
	this.d = d;
    }

    @Override
    protected void deleteDominated(float[] p) {
	if (root != null) {
	    _deleteDominated(root, p);
	}
    }

    // deletes all points in the subtree rooted at r that are dominated by v
    private void _deleteDominated(Node r, float[] v) {
	// First, clean up r's children.
	if (r.numberOfChildren > 0) {
	    for (int i = r.idOfFirstChild; i < d; i++) {
		Node childI = r.children[i];
		if (childI != null) {
		    _deleteDominated(childI, v);
		}
		if (v[i] < r.point[i]) {
		    break;
		}
	    }
	}

	// Now, check if r.point itself is dominated by v.
	PointRelationship dom = PointComparator.compare(v, r.point);
	numComp++;
	if (dom == PointRelationship.DOMINATES) {
	    // r.point is dominated by v, so let's delete r
	    Node rParent = r.parent;
	    if (r.numberOfChildren == 0) {
		// r is a leaf node
		if (rParent == null) {
		    // r is the root node
		    root = null;
		} else {
		    unlinkChild(rParent, r.childID);
		}
	    } else {
		// r is not a leaf node.
		// The first child of r will replace r
		Node rReplace = r.children[r.idOfFirstChild];
		if (rParent == null) {
		    // r is the root node
		    root = rReplace;
		} else {
		    // r is an ordinary node
		    rParent.children[r.childID] = rReplace;
		}
		rReplace.parent = rParent;
		rReplace.childID = r.childID;

		// Reinsert all points located in the remaining children.
		for (int i = r.idOfFirstChild + 1; i < d; i++) {
		    Node childI = r.children[i];
		    if (childI != null) {
			_treeInsert(rReplace, childI);
		    }
		}
	    }
	}
    }


    @Override
    public boolean isDominated(float[] p) {
	if (root == null) {
	    return false;
	} else {
	    return _isDominated(root, p);
	}
    }

    // Returns true iff some point in the tree rooted at r dominates v
    private boolean _isDominated(Node r, float[] v) {
	PointRelationship dom = PointComparator.compare(r.point, v);
	numComp++;
	if (dom == PointRelationship.DOMINATES) {
	    // r.point dominates v
	    return true;
	} else {
	    if (r.numberOfChildren > 0) {
		for (int i = r.idOfFirstChild; i < d; i++) {
		    // check children of r
		    Node childI = r.children[i];
		    if ((childI != null) && (r.point[i] >= v[i])) {
			if (_isDominated(childI, v)) {
			    return true;
			}
		    }
		}
	    }
	    return false;
	}
    }


    @Override
    protected void insert(float[] p) {
	if (root == null) {
	    root = root = new Node(p, null, -1);
	} else {
	    _insert(root, p);
	}
    }

    // Inserts v into the tree r
    private static void _insert(Node r, float[] v) {
	int i = 0;
	while (v[i] >= r.point[i]) {
	    i++;
	}
	// now, i is the first dimension wrt. which r.point is greater than v
	if (r.children[i] != null) {
	    _insert(r.children[i], v);
	} else {
	    r.children[i] = new Node(v, r, i);
	    if (r.numberOfChildren == 0) {
		r.idOfFirstChild = i;
	    } else {
		if (i < r.idOfFirstChild) {
		    r.idOfFirstChild = i;
		}
	    }
	    r.numberOfChildren++;
	}
    }

    // Inserts every point in the tree s into the tree r
    private void _treeInsert(Node r, Node s) {
	if (s.numberOfChildren > 0) {
	    for (int i = s.idOfFirstChild; i < d; i++) {
		Node childI = s.children[i];
		if (childI != null) {
		    _treeInsert(r, childI);
		}
	    }
	}
	_insert(r, s.point);
    }


    @Override
    public long getNumberOfComparions() {
	return numComp;
    }

    

    

    private static void unlinkChild(Node n, int childID) {
	n.children[childID] = null;
	n.numberOfChildren--;
	if ((childID == n.idOfFirstChild) && (n.numberOfChildren > 0)) {
	    // We need to update n.idOfFirstChild.
	    int i = n.idOfFirstChild + 1;
	    while (n.children[i] == null) {
		i++;
	    }
	    n.idOfFirstChild = i;
	}
    }


    @Override
    public Iterator<float[]> iterator() {
	return new DominanceDecisionTreeIterator(this);
    }

    @Override
    public String deepToString() {
	StringBuffer result = new StringBuffer();
	DominanceDecisionTreeIterator iter = new DominanceDecisionTreeIterator(this);
	while (iter.hasNext()) {
	    int level = iter.getCurrentLevel();
	    int id = iter.getChildIdOfNextItem();
	    String idStr = "";
	    if (id != -1) {
		idStr = "(" + Integer.toString(id) + ")";
	    }
	    float[] next = iter.next();
	    result.append(String.format("%s%s %s%n", StringUtils.repeat(' ', level * 5), idStr, Arrays.toString(next)));
	}
	return result.toString();
    }

    @Override
    public String getStats() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getNumberOfNodesVisited() {
	return -1;
    }

    @Override
    public void addToNumberOfComparisons(long num) {
	numComp += num;
    }

    private static class Node {
	private final float[] point;
	private final Node[] children;
	private Node parent;
	private int childID;
	private int numberOfChildren = 0;
	private int idOfFirstChild;

	// creates an empty node
	private Node(float[] point, Node parent, int childID) {
	    this.point = Arrays.copyOf(point, point.length);
	    children = new Node[point.length];
	    this.parent = parent;
	    this.childID = childID;
	}
    }

    private class DominanceDecisionTreeIterator implements Iterator<float[]> {

	private ArrayListStack<Node> stack;
	private ArrayListIntStack<Node> stackInt;

	private DominanceDecisionTreeIterator(DominanceDecisionTree tree) {
	    stack = new ArrayListStack<Node>();
	    stackInt = new ArrayListIntStack<Node>();
	    stack.push(tree.root);
	}

	@Override
	public boolean hasNext() {
	    return !stack.isEmpty();
	}

	@Override
	public float[] next() {
	    Node currentNode = stack.pop();
	    float[] result = currentNode.point;
	    if (currentNode.numberOfChildren > 0) {
		// traverse to first child of currentNode
		stack.push(currentNode);
		stack.push(currentNode.children[currentNode.idOfFirstChild]);
		stackInt.push(currentNode.idOfFirstChild);
	    } else {
		// traverse to next sibling of currentNode
		while (!stack.isEmpty()) {
		    // find next child
		    Node parentOfCurrentNode = stack.pop();
		    int childIdOfCurrentNode = stackInt.pop();
		    int i = childIdOfCurrentNode + 1;
		    while ((i < d) && (parentOfCurrentNode.children[i] == null)) {
			i++;
		    }
		    if (i == d) {
			// there is no next child
			currentNode = parentOfCurrentNode;
		    } else {
			stack.push(parentOfCurrentNode);
			stack.push(parentOfCurrentNode.children[i]);
			stackInt.push(i);
			break;
		    }
		}
	    }
	    return result;
	}

	private int getCurrentLevel() {
	    return stackInt.size();
	}

	private int getChildIdOfNextItem() {
	    if (stackInt.isEmpty()) {
		return -1;
	    } else {
		int id = stackInt.pop();
		stackInt.push(id);
		return id;
	    }
	}

	@Override
	public void remove() {
	    throw new UnsupportedOperationException("Not supported yet.");
	}
    }
}
