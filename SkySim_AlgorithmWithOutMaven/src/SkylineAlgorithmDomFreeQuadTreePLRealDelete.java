


public class SkylineAlgorithmDomFreeQuadTreePLRealDelete extends AbstractSkylineAlgorithmDomFreeTree {

    @Override
    public String toString() {
	return "QuadTreePLReal" ;
    }

    @Override
    public String getShortName() {
	return "QuadTreePLReal";
    }

    @Override
    protected DomFreeTree _createTree(int d) {
	return new DomFreeQuadTreePLRealDelete(d);
    }
}