

import java.util.Arrays;
import java.util.List;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

public abstract class AbstractSkylineAlgorithmDomFreeTree extends AbstractSkylineAlgorithm {
    protected boolean presorted;
    private final boolean debug;
    private DomFreeTree lastTree;
    
    // NOTE: The default seed is used here!
    private RandomEngine re = new MersenneTwister();

    public AbstractSkylineAlgorithmDomFreeTree() {
	this(false);
    }

    public AbstractSkylineAlgorithmDomFreeTree(boolean debug) {
	this.debug = debug;
    }

    @Override
    public void setExperimentConfig(SimulatorConfiguration config) {
	super.setExperimentConfig(config);
	presorted = ((config.getPresortStrategy() == SimulatorConfiguration.PresortStrategy.FullPresortSum)
		|| (config.getPresortStrategy() == SimulatorConfiguration.PresortStrategy.FullPresortVolume));
    }

    @Override
    public List<float[]> compute(PointSource data) {
	ioCost = 0;
	reorgTimeNS = -1;
	long startTime = System.nanoTime();

	int d = data.getD();
	DomFreeTree tree = _createTree(d);
	lastTree = tree;
	final int n = data.size();

	if (presorted && (tree instanceof AbstractDomFreeQuadTreePL)) {
	    // Randomly select the first data point to be inserted.
	    float[] firstSkylinePoint = _findUndominatedRandomPresorted(data);
	    tree.simpleInsertIfUndominated(firstSkylinePoint);
	}

	for (int i = 0; i < n; i++) {
	    float[] dataPoint = data.get(i);
	    ioCost++;
	    if (presorted) {
		tree.simpleInsertIfUndominated(dataPoint);
	    } else {
		tree.dominationFreeInsertIfUndominated(dataPoint);
	    }
	}
	cpuCost = tree.getNumberOfComparions();

	numberOfNodesVisited = tree.getNumberOfNodesVisited();

	totalTimeNS = System.nanoTime() - startTime;

	memoryConsumption = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

	LinkedPointList result = new LinkedPointList(d);
	for (float[] point: tree) {
	    result.add(point);
	}

	if (debug) {
	    System.out.println(tree.getStats());
	}

	return result;
    }

    // Finds some skyline point in a presorted point source;
    // start with some random point.
    private float[] _findUndominatedRandomPresorted(PointSource ds) {
	// equality handling not supported yet
	float[] data = ds.toFlatArray();
	int n = ds.size();
	int d = ds.getD();
	
	// Randomly generate a starting point.
	int i = re.nextInt() % n;
	if (i < 0) {
	    i += n;
	}

	int numOps = 0;

	int pos = i * d;
	float[] candidate = Arrays.copyOfRange(data, pos, pos + d);
	dataloop:
	for (int posDataStart = pos - 1; posDataStart >= 0; posDataStart -= d) {
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
	
	lastTree.addToNumberOfComparisons(numOps);

	return candidate;
    }

    protected abstract DomFreeTree _createTree(int d);
}
