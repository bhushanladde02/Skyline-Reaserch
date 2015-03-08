


public class SkylineAlgorithmDominanceDecisionTree extends AbstractSkylineAlgorithmDomFreeTree {

    @Override
    public String toString() {
	return "DDTree" ;
    }

    @Override
    public String getShortName() {
	return "DDTree";
    }

    @Override
    protected DomFreeTree _createTree(int d) {
	return new DominanceDecisionTree(d);
    }
}
