


public class DomFreeQuadTreePLLazyDelete extends AbstractDomFreeQuadTreePL {

    public DomFreeQuadTreePLLazyDelete(int d) {
	super(d);
    }

    @Override
    protected void deleteDominated(float[] p) {
	if (root != null) {
	    _deleteDominatedLazy(root, p);
	}
    }

    private void _deleteDominatedLazy(Node e, float[] o) {
	// For any dimension i: o[i] < eParent[i] ==> e[i] < eParent[i].
	// In other words: There is no i such that o[i] < eParent[i] <= e[i].
	// Therefore, o could possibly dominate e.
	long Pa2 = PointComparator.getRegionIDOfBRelativeToAZhang2(e.point, o, d);
	numComps++;
	if (Pa2 == 0) {
	    // o dominates e.
	    e.deleted = true;
	}
	// Check e's children.
	Node son = e.firstSon;
	while ((son != null) && (son.pa2 < Pa2)) {
	    numberOfNodesVisited++;
	    son = son.nextSibling;
	}
	while (son != null) {
	    numberOfNodesVisited++;
	    if ((son.pa2 & Pa2) == Pa2) {
		// o possibly dominates the next sibling of e.
		_deleteDominatedLazy(son, o);
	    }
	    son = son.nextSibling;
	}
    }
}