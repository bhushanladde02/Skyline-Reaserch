


public class SkylineAlgorithmDomFreeQuadTreePLLazyDelete extends AbstractSkylineAlgorithmDomFreeTree {

    public SkylineAlgorithmDomFreeQuadTreePLLazyDelete(boolean debug) {
	super(debug);
    }

    @Override
    public String toString() {
	return "QuadTreePLLazy" ;
    }

    @Override
    public String getShortName() {
	return "QuadTreePLLazy";
    }

    @Override
    protected DomFreeTree _createTree(int d) {
	return new DomFreeQuadTreePLLazyDelete(d);
    }
}