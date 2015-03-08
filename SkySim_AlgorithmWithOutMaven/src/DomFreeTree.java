

public interface DomFreeTree extends Iterable<float[]> {
    public void simpleInsertIfUndominated(float[] p);
    public void dominationFreeInsertIfUndominated(float[] p);
    public boolean isDominated(float[] p);
    public long getNumberOfComparions();
    public long getNumberOfNodesVisited();
    public String deepToString();
    public String getStats();
    public void addToNumberOfComparisons(long num);
}