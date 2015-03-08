

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class SkylineAlgorithm_RDF_sorted extends AbstractSkylineAlgorithm {

    private PointComparator_RDF comparator;
    private boolean usekmode = true;
    private AbstractSkylineAlgorithm innerskyline = null;

    public SkylineAlgorithm_RDF_sorted(PointComparator_RDF comparator, AbstractSkylineAlgorithm innerskyline) {
        super();
        this.comparator = comparator;
        this.innerskyline = innerskyline;
    }

    @Override
    public void setExperimentConfig(SimulatorConfiguration config) {
        super.setExperimentConfig(config);
    }

    /**
     * 
     * @param data
     * @return 
     */
    @Override
    public synchronized List<float[]> compute(PointSource data) {


        int d = data.getD();


        Collections.sort(data, new Comparator1N());
        List<float[]> result = innerskyline.compute(data);
        ioCost = innerskyline.getIOcost();
        cpuCost = innerskyline.getCPUcost();
        totalTimeNS = innerskyline.getTotalTimeNS();


        //
        return result;
    }

    private LinkedPointList_RDF_nm copyPointList(PointList source) {
        LinkedPointList_RDF_nm result = new LinkedPointList_RDF_nm(source.getD(), comparator);
        PointListIterator iter = source.listIterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

    private LinkedPointList_RDF_nm copyPointSource(PointSource source) {
        LinkedPointList_RDF_nm result = new LinkedPointList_RDF_nm(source.getD(), comparator);
        Iterator<float[]> iter = source.iterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("RDF nm-dominance Sorted " + comparator.toString());
        return buf.toString();
    }

    @Override
    public String getShortName() {
        return "RDF_N1_Sorted";
    }

    public class Comparator1N implements Comparator<float[]> {

        @Override
        public int compare(float[] o1, float[] o2) {
            float o1d = o1[o1.length - 1];
            float o2d = o2[o2.length - 1];
            if (o1d > o2d) {
                return -1;
            }
            if (o2d > o1d) {
                return 1;
            }
            return 0;
        }
    }
}
