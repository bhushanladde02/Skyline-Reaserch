

import java.util.List;

/**
 *
 * @author selke
 */
public class DummyReorganizer extends AbstractReorganizer {

    public DummyReorganizer(List<SkylineWorker> workers) {
        super(workers);
    }

    public DummyReorganizer() {
        super();
    }

    @Override
    protected void reorganize() {
    }
    
        public String toString() {
	    return "Dummy Reorganizer";
    }
}
