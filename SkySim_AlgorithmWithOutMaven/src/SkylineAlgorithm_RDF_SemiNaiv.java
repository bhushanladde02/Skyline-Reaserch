

import java.util.Iterator;
import java.util.List;

public final class SkylineAlgorithm_RDF_SemiNaiv extends AbstractSkylineAlgorithm {

    private PointComparator_RDF comparator;

    public SkylineAlgorithm_RDF_SemiNaiv(PointComparator_RDF comparator) {
        super();
        this.comparator = comparator;
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
        long startTime = System.nanoTime();

        int d = data.getD();
        ListOrder listOrder;
        listOrder = ListOrder.Unsorted;

        final PointList testset = copyPointSource(data);
        final PointList testset_looser = copyPointSource(data);
        final PointList skyline = new LinkedPointList_RDF_nm(d, comparator);
        final PointList looser = new LinkedPointList_RDF_nm(d, comparator);

        // number of disk accesses
        ioCost = 0;
        int cpuCost = 0;
        int insertions = 0;
        int deletions = 0;
        int moves = 0;
        int drops = 0;


        BNLProfiler profiler = new BNLProfiler();
        final int n = data.size();

        datalist:
        while (testset_looser.size() > 0) {
            float[] small_t = testset_looser.get(0);
            final PointList compare = copyPointList(testset);
            compare.remove(small_t); // hier müsste small_t hin
            boolean failed = false;
            while (compare.size() > 0) {
                float[] small_c = compare.get(0);
                PointRelationship compareresult = comparator.compare(small_t, small_c);
                cpuCost++;
                if (compareresult.equals(PointRelationship.DOMINATES)) {
                    looser.add(small_c);
                    testset_looser.remove(small_c);

                }
                if (compareresult.equals(PointRelationship.IS_DOMINATED_BY)) {
                    looser.add(small_t);
                    testset_looser.remove(small_t);
                    failed = true;
                }
                compare.remove(0);

            }
            if (!failed) {
                skyline.add(small_t);
            }
            testset.remove(small_t);
            testset_looser.remove(small_t);


        }

        //
        if (profiler != null) {
            BNLProfiler.updateProfiler(profiler, insertions, deletions, moves, drops, cpuCost);
        }

        this.cpuCost = profiler.getCpuCost();
        this.totalTimeNS = System.nanoTime() - startTime;

        //


        //
        return skyline;
    }

    private PointList copyPointList(PointList source) {
        PointList result = new LinkedPointList(source.getD());
        PointListIterator iter = source.listIterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

    private PointList copyPointSource(PointSource source) {
        PointList result = new LinkedPointList(source.getD());
        Iterator<float[]> iter = source.iterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("RDF nm-dominance SEMI-NAIVE " + comparator.toString());
        return buf.toString();
    }

    @Override
    public String getShortName() {
        return "RDF_NM_SN";
    }
}
