/*
 * pskyline algorithm presented in
 * Park, Kim, Park, Kim, Im: Parallel Skyline Computation on Multicore Architectures
 */



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SkylineAlgorithmPskyline extends AbstractSkylineAlgorithm {

    private static final int TRUE = 1;
    private static final int FALSE = 0;

    private int numCPUs;
    private int numBlocks;

    @Override
    public void setExperimentConfig(SimulatorConfiguration config) {
	super.setExperimentConfig(config);
	numCPUs = config.getNumberOfCPUs();
	numBlocks = config.getDistributedNumBlocks();
    }

    @Override
    public List<float[]> compute(PointSource data) {
	// In disk-based applications, numBlock should be chosen
	// such that numCPUs * blocksize tuples fit in main memory.
	//
	// Here, we choose a blocksize that results in exactly numCPUs blocks.
	long startTime = System.nanoTime();
	
	List<float[]> result = compute(data, numBlocks);

	totalTimeNS = System.nanoTime() - startTime - preprocessTimeNS;
	
	return result;
    }

    private List<float[]> compute(PointSource data, int numBlocks) {
	cpuCost = 0;
	ioCost = 0;
	int n = data.size();
	double blocksize = (double) n / numBlocks;

	// calculate block borders
	int[] froms = new int[numBlocks];
	int[] tos = new int[numBlocks];
	froms[0] = 0;
	tos[0] = (int) Math.round(blocksize);
	for (int i = 1; i < numBlocks; i++) {
	    froms[i] = tos[i - 1];
	    tos[i] = (int) Math.round(i * blocksize);
	}
	tos[numBlocks - 1] = n;

	// create and submit computation tasks, i.e.
	// compute individual skylines using sskyline in parallel
	long start = System.nanoTime();
	ExecutorService executor = Executors.newFixedThreadPool(numCPUs);
	List<SskylineTask> tasks = new ArrayList<SskylineTask>(numBlocks);
	for (int i = 0; i < numBlocks; i++) {
	    tasks.add(new SskylineTask(data, froms[i], tos[i]));
	}
	List<Future<ArrayPointList>> results;
	try {
	    results = executor.invokeAll(tasks);
	} catch (InterruptedException ex) {
	    Logger.getLogger(SkylineAlgorithmPskyline.class.getName()).log(Level.SEVERE, null, ex);
	    throw new UnsupportedOperationException("Error handling not supported yet.");
	}

	long end = System.nanoTime();
	System.out.format("Computing %d local skylines: %5.2f ms  ...  ", numBlocks, (end - start) / 1000000.0);

	// calculate average preprocessing time and substract it from the total time
	long preprocessTimeTotalNS = 0;
	for (int i = 0; i < numBlocks; i++) {
	    SskylineTask task = tasks.get(i);
	    preprocessTimeTotalNS += task.getPreprocessTimeNS();
	    cpuCost += task.getCPUCost();
	}
	preprocessTimeNS = preprocessTimeTotalNS / numBlocks;

	// get first result and use it as intermediate result while merging
	start = System.nanoTime();
	Iterator<Future<ArrayPointList>> iter = results.iterator();
	ArrayPointList intermediateResult;
	try {
	    intermediateResult = iter.next().get();
	} catch (Exception ex) {
	    Logger.getLogger(SkylineAlgorithmPskyline.class.getName()).log(Level.SEVERE, null, ex);
	    throw new UnsupportedOperationException("Error handling not supported yet.");
	}

	// merge results
	AtomicInteger cpuCostMerge = new AtomicInteger();
	while (iter.hasNext()) {
	    ArrayPointList nextResult = null;
	    try {
		nextResult = iter.next().get();
	    } catch (Exception ex) {
		Logger.getLogger(SkylineAlgorithmPskyline.class.getName()).log(Level.SEVERE, null, ex);
		throw new UnsupportedOperationException("Error handling not supported yet.");
	    }
	    intermediateResult = parallel_merge(executor, intermediateResult, nextResult, numCPUs, cpuCostMerge);
	}
	cpuCost += cpuCostMerge.get();

	executor.shutdown();
	try {
	    executor.awaitTermination(1, TimeUnit.DAYS);
	} catch (InterruptedException ex) {
	    Logger.getLogger(SkylineAlgorithmPskyline.class.getName()).log(Level.SEVERE, null, ex);
	    throw new UnsupportedOperationException("Error handling not supported yet.");
	}

	end = System.nanoTime();
	System.out.format("Merging: %5.2f ms%n", (end - start) / 1000000.0);

	// return final result
	return intermediateResult;
    }

    /*
     * In parallel, merges two skyline sets into a common skyline set.
     * See Park, Kim, Park, Kim, Im: Parallel Skyline Computation on Multicore Architectures
     * (pmerge, Figure 3)
     */
    private static ArrayPointList parallel_merge(final ExecutorService executor, final ArrayPointList s1, final ArrayPointList s2, final int numCPUs, final AtomicInteger cpuCostMerge) {
	final int n1 = s1.size();
	final int n2 = s2.size();
	final int[] t1Base = new int[n1];
	Arrays.fill(t1Base, TRUE);
	final int[] t2Base = new int[n2];
	final AtomicIntegerArray t1 = new AtomicIntegerArray(t1Base);
	final AtomicIntegerArray t2 = new AtomicIntegerArray(t2Base);
	final AtomicInteger i2a = new AtomicInteger();

	final Collection<Callable<Object>> mergers = new ArrayList<Callable<Object>>(numCPUs);
	for (int i = 0; i < numCPUs; i++) {
	    Callable<Object> merger = new MergeTask(s1, s2, t1, t2, i2a, cpuCostMerge);
	    mergers.add(merger);
	}
	try {
	    executor.invokeAll(mergers);
	} catch (InterruptedException ex) {
	    Logger.getLogger(SkylineAlgorithmPskyline.class.getName()).log(Level.SEVERE, null, ex);
	}

	// now, the following is true:
	// t1[i] == true  iff  s1[i] is contained in the merged skyline
	// t2[i] == true  iff  s2[i] is contained in the merged skyline

	// condense result (very fast comparable to the rest; no need to optimize here)
	int d = s1.getD();
	ArrayPointList result = new ShiftingArrayPointList(n1 + n2, d);
	for (int i1 = 0; i1 < n1; i1++) {
	    if (t1.get(i1) == TRUE) {
		result.add(s1.get(i1));
	    }
	}
	for (int i2 = 0; i2 < n2; i2++) {
	    if (t2.get(i2) == TRUE) {
		result.add(s2.get(i2));
	    }
	}

	return result;
    }

    private static class MergeTask implements Callable<Object> {

	private final ArrayPointList s1;
	private final ArrayPointList s2;
	private final AtomicIntegerArray t1;
	private final AtomicIntegerArray t2;
	private final AtomicInteger ai2;
	private final int n1;
	private final int n2;
	private final AtomicInteger cpuCostMerge;

	private MergeTask(ArrayPointList s1, ArrayPointList s2, AtomicIntegerArray t1, AtomicIntegerArray t2, AtomicInteger ai2, AtomicInteger cpuCostMerge) {
	    this.s1 = s1;
	    this.s2 = s2;
	    this.t1 = t1;
	    this.t2 = t2;
	    this.ai2 = ai2;
	    this.cpuCostMerge = cpuCostMerge;
	    n1 = t1.length();
	    n2 = t2.length();
	}

	@Override
	public Object call() {
	    int cpuCost = 0;
	    int i2;
	    s2loop:
	    while ((i2 = ai2.getAndIncrement()) < n2) {
		final float[] p2 = s2.get(i2);
	    	// compare p2 == s2[i2] to all currently non-dominated points in s1
		// mark dominated points in s1
		// mark non-dominated points in s2
		for (int i1 = 0; i1 < n1; i1++) {
		    if (t1.get(i1) == TRUE) {
			// s1[i1] is still undominated
			// compare s1[i1] to s2[i2]
			PointRelationship dom = s1.compare(i1, p2);
			cpuCost++;
			switch (dom) {
			    case DOMINATES:
				// skip to next point in s2
				continue s2loop;
			    case IS_DOMINATED_BY:
				// s1[i1] is dominated by s2[i2]
				t1.set(i1, FALSE);
				break;
			}
		    }
		}
		// s2[i2] is not dominated by s1
		t2.set(i2, TRUE);
	    }
	    cpuCostMerge.getAndAdd(cpuCost);
	    return null;
	}
    }

    private static class SskylineTask implements Callable<ArrayPointList> {

	private final int from;
	private final int to;
	private final PointSource data;
	private volatile long timePreprocessNS;
	private volatile long cpuCost;

	private SskylineTask(PointSource data, int from, int to) {
	    this.data = data;
	    this.from = from;
	    this.to = to;
	}

	@Override
	public ArrayPointList call() {
	    SkylineAlgorithmSskyline alg = new SkylineAlgorithmSskyline();
	    ArrayPointList result = alg.compute(data, from, to);
	    timePreprocessNS = alg.getPreprocessTimeNS();
	    cpuCost = alg.getCPUcost();
	    return result;
	}

	public long getPreprocessTimeNS() {
	    return timePreprocessNS;
	}

	public long getCPUCost() {
	    return cpuCost;
	}
    }

    @Override
    public String toString() {
	return String.format("pskyline (%d %d)", numCPUs, numBlocks);
    }

    @Override
    public String getShortName() {
	return toString();
    }
}
