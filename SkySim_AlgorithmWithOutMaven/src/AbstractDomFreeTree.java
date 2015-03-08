

public abstract class AbstractDomFreeTree implements DomFreeTree {
    @Override
    public void simpleInsertIfUndominated(float[] p) {
	if (!isDominated(p)) {
	    insert(p);
	}
    }

    @Override
    public void dominationFreeInsertIfUndominated(float[] p) {
	if (!isDominated(p)) {
	    deleteDominated(p);
	    insert(p);
	}
    }

    protected abstract void deleteDominated(float[] p);

    protected abstract void insert(float[] p);
}
