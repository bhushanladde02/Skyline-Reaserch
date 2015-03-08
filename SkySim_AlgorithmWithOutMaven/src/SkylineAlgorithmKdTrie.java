

import java.util.List;

public class SkylineAlgorithmKdTrie extends AbstractSkylineAlgorithmDomFreeTree {

    private KdTrie lastTree;
    private final boolean debug;

    public SkylineAlgorithmKdTrie(boolean debug) {
	super();
	this.debug = debug;
    }

    @Override
    public List<float[]> compute(PointSource data) {
	List<float[]> result = super.compute(data);
	if (debug) {
	    System.out.println(lastTree.getStats());
	}
	return result;
    }

    @Override
    public String toString() {
	return "kd-trie" ;
    }

    @Override
    public String getShortName() {
	return "kd-trie";
    }

    @Override
    protected DomFreeTree _createTree(int d) {
	lastTree = new KdTrie(d);
	return lastTree;
    }
}
