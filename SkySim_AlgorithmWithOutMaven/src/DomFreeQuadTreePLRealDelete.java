


public class DomFreeQuadTreePLRealDelete extends AbstractDomFreeQuadTreePL {

    public DomFreeQuadTreePLRealDelete(int d) {
	super(d);
    }

    @Override
    protected void deleteDominated(float[] p) {
	if (root != null) {
	    numberOfNodesVisited++;
	    _deleteDominated(null, root, p);
	}
    }

    private void _deleteDominated(Node eLeftSibling, Node e, float[] o) {
	// For any dimension i: o[i] < eParent[i] ==> e[i] < eParent[i].
	// In other words: There is no i such that o[i] < eParent[i] <= e[i].
	// Therefore, o could possibly dominate e.
	long Pa2 = PointComparator.getRegionIDOfBRelativeToAZhang2(e.point, o, d);
	numComps++;
	if (Pa2 == 0) {
	    // o dominates e.
	    e.deleted = true; // needed for relinking below

	    // Let's reinsert all undominated descendants of e at e's position.
	    // Begin with finding the first undominated descendant. It will provide e's replacement.
	    Node desc = e.firstSon;
	    // Let the getNextOf routine think that e is the root node.
	    Node eParent = e.parent;
	    Node eNextSibling = e.nextSibling;
	    e.parent = null;
	    e.nextSibling = null;
	    while (desc != null) {
		numberOfNodesVisited++;
		if (PointComparator.compare(o, desc.point) != PointRelationship.DOMINATES) {
		    break;
		}
		desc = TreeIterator.getNextOf(desc);
		// plus some more visited nodes (we cannot count here) ...
	    }

	    if (desc == null) {
		// No replacement for e has been found. Let's just unlink e.
		if (eLeftSibling == null) {
		    // e is the first son of its parent.
		    if (e == root) {
			root = null;
		    } else {
			eParent.firstSon = eNextSibling;
		    }
		} else {
		    eLeftSibling.nextSibling = eNextSibling;
		}
	    } else {
		// desc replaces e.
		Node eNew = new Node(desc.point, null, eNextSibling, eParent);
		numberOfNodesVisited++;
		if (eLeftSibling == null) {
		    // e is the first son of its parent.
		    if (e == root) {
			root = eNew;
		    } else {
			eParent.firstSon = eNew;
		    }
		} else {
		    eLeftSibling.nextSibling = eNew;
		}
		// Reinsert all remaining nondominated descendants of e.
		desc = TreeIterator.getNextOf(desc);
		while (desc != null) {
		    numberOfNodesVisited++;
		    numComps++;
		    if (PointComparator.compare(o, desc.point) != PointRelationship.DOMINATES) {
			_insert(eNew, desc.point);
		    }
		    desc = TreeIterator.getNextOf(desc);
		}
	    }
	} else {
	    // o does not dominates e.
	    // Check e's children.
	    Node sonLeftSibling = null;
	    Node son = e.firstSon;
	    while ((son != null) && (son.pa2 < Pa2)) {
		numberOfNodesVisited++;
		sonLeftSibling = son;
		son = son.nextSibling;
	    }
	    while (son != null) {
		numberOfNodesVisited++;
		if ((son.pa2 & Pa2) == Pa2) {
		    // o possibly dominates the next sibling of e.
		    _deleteDominated(sonLeftSibling, son, o);
		    if (son.deleted) {
			if (sonLeftSibling == null) {
			    son = e.firstSon;
			} else {
			    son = sonLeftSibling.nextSibling;
			}
		    } else {
			sonLeftSibling = son;
			son = son.nextSibling;
		    }
		} else {
		    sonLeftSibling = son;
		    son = son.nextSibling;
		}
	    }
	}
    }
}
