

import java.util.List;

public class SkylineAlgorithmKdTriePartition extends AbstractSkylineAlgorithm {

    private boolean presorted;
    private final boolean debug;

    public SkylineAlgorithmKdTriePartition() {
	this(false);
    }

    public SkylineAlgorithmKdTriePartition(boolean debug) {
	this.debug = debug;
    }


    @Override
    public void setExperimentConfig(SimulatorConfiguration config) {
	super.setExperimentConfig(config);
	presorted = ((config.getPresortStrategy() == PresortStrategy.FullPresortSum)
		|| (config.getPresortStrategy() == PresortStrategy.FullPresortVolume));
    }

    @Override
    public List<float[]> compute(PointSource data) {
	ioCost = -1;
	reorgTimeNS = -1;
	long startTime = System.nanoTime();

	int d = data.getD();
	KdTrie tree = new KdTrie(data, presorted);
	totalTimeNS = System.nanoTime() - startTime;

	memoryConsumption = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	cpuCost = tree.getNumberOfComparions();
	numberOfNodesVisited = tree.getNumberOfNodesVisited();

	LinkedPointList result = new LinkedPointList(d);
	for (float[] point: tree) {
	    result.add(point);
	}

	if (debug) {
	    System.out.println(tree.getStats());
	}

	return result;
    }

    @Override
    public String toString() {
	return "KdTriePart" ;
    }

    @Override
    public String getShortName() {
	return "KdTriePart";
    }
}
