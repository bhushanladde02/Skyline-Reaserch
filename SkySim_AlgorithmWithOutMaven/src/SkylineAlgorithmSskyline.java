/*
 * sskyline algorithm presented in
 * Park, Kim, Park, Kim, Im: Parallel Skyline Computation on Multicore Architectures
 * (Figure 4)
 *
 * This algorithm is very similar to Ciaccia's Best algorithm
 * (which always holds the current maximum and returns it progressively after all comparisons).
 * But Sskyline uses are more clever memory management. Dominated elements already are replaced
 * by the current input list's tail. Therefore, the current maximum always lies at the first position,
 * the current comparison elements wanders along the list until the end is reached; then then first position
 * is returned.
 *
 * Sskyline is an in-place algorithm and relies on an array representation of the input data.
 */



import java.util.List;

public class SkylineAlgorithmSskyline extends AbstractSkylineAlgorithm {

    // TODO: handling of duplicates

    @Override
    public List<float[]> compute(PointSource data) {
	return compute(data, 0, data.size());
    }

    public ArrayPointList compute(PointSource data, int from, int to) {
	cpuCost = 0;
	ioCost = 0;

	long startPreprocess = System.nanoTime();
	ArrayPointList dataArrayX = preprocess(data, from, to);
	long endPreprocess = System.nanoTime();
	preprocessTimeNS = endPreprocess - startPreprocess;

	return compute(dataArrayX, from, to);
    }

    public static ArrayPointList preprocess(PointSource data, int from, int to) {
	int d = data.getD();
	int n = data.size();
	ArrayPointList dataArrayX = new ShiftingArrayPointList(n, d);
	for (int i = from; i < to; i++) {
	    float[] point = data.get(i);
	    dataArrayX.add(point);
	}
	return dataArrayX;
    }

    /*
     * In-place compututation of the skyline on some float point array,
     * returns the number of skyline points (which are located at the beginning of the array).
     */
    public static int performSskylineRun(float[] dataArrayX, int d, int posFrom, int posTo) {
	int head = posFrom; // pointer to first element that has not been identified as skyline point yet
	int tail = posTo - d; // pointer to last element of the list to be checked

	while (head < tail) {
	    int pos = head + d;
	    while (pos <= tail) {
		PointRelationship dom = PointComparator.compare(dataArrayX, head, pos, d);
		switch (dom) {
		    case DOMINATES:
			// tail --> pos
			System.arraycopy(dataArrayX, tail, dataArrayX, pos, d);
			tail -= d;
			break;
		    case IS_DOMINATED_BY:
			// pos --> head
			// tail --> pos
			System.arraycopy(dataArrayX, pos, dataArrayX, head, d);
			System.arraycopy(dataArrayX, tail, dataArrayX, pos, d);
			tail -= d;
			pos = head + d;
			break;
		    case IS_INCOMPARABLE_TO:
			pos += d;
			break;
		}
	    }
	    if (head < tail) {
		head += d;
	    }
	}

	return head / d + 1;
    }
    

    public ArrayPointList compute(ArrayPointList dataArrayX, int from, int to) {
	cpuCost = 0;
	ioCost = 0;
	long startTime = System.nanoTime();

	int d = dataArrayX.getD();
	int n = dataArrayX.size();

	int head = 0;
	int tail = n - 1;

	while (head < tail) {
	    int i = head + 1;
	    while (i <= tail) {
		PointRelationship dom = dataArrayX.compare(head, i);
		ioCost += 2;
		cpuCost++;
		switch (dom) {
		    case DOMINATES:
			dataArrayX.copy(tail, i);
			ioCost++;
			tail--;
			break;
		    case IS_DOMINATED_BY:
			dataArrayX.copy(i, head);
			ioCost++;
			dataArrayX.copy(tail, i);
			ioCost++;
			tail--;
			i = head + 1;
			break;
		    case IS_INCOMPARABLE_TO:
			i++;
			break;
		}
	    }
	    if (head < tail) {
		head++;
	    }
	}

	totalTimeNS = System.nanoTime() - startTime;

	return new ShiftingArrayPointList(dataArrayX.getSubarray(0, head + 1), d, false);
    }

    @Override
    public String toString() {
	return "sskyline";
    }

    @Override
    public String getShortName() {
	return "sskyline";
    }

}