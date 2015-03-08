

import java.util.Iterator;
import java.util.List;

public final class SkylineAlgorithm_RDF_SemiNaiv_Flags_V2 extends AbstractSkylineAlgorithm {

    private PointComparator_RDF comparator;
    private boolean usekmode = true;

    public SkylineAlgorithm_RDF_SemiNaiv_Flags_V2(PointComparator_RDF comparator) {
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
        final LinkedPointList_RDF_nm datalist = copyPointSource(data);
        LinkedPointListIterator iter_testMlooser = (LinkedPointListIterator) datalist.iterator();


        // number of disk accesses
        ioCost = 0;
        cpuCost = 0;
        int insertions = 0;
        int deletions = 0;
        int moves = 0;
        int drops = 0;
        int kcostcounter = 0;


        BNLProfiler profiler = new BNLProfiler();
        final int n = data.size();

        datalist_loop:
        while (true) {
            // get next one on test \ looser; break if there is none
            float[] small_t = iter_testMlooser.nextInTestMloosers();
            if (small_t == null) {
                break datalist_loop;
            }
            // new iterator for compare, skipp small_t: Günntzer Addon 2: zuerst die non looser
            LinkedPointListIterator iter_compare = (LinkedPointListIterator) datalist.iterator();
            boolean failed = false;
            // loop compare
            compare_loop:
            while (true) {
                // break if there is no next small c
                float[] small_c = null;
                small_c = iter_compare.nextInTestButNotThisAndNoLooser(iter_testMlooser.getDataNode());
                // exit of C is empty
                if (small_c == null) {
                    break compare_loop;
                }
                //
                PointRelationship compareresult = comparator.compare(small_t, small_c);
                cpuCost++;
                if (compareresult.equals(PointRelationship.DOMINATES)) {
                    iter_compare.setInLoosers(true);
                    iter_compare.setNewInLoosers(true);
                }
                if (compareresult.equals(PointRelationship.IS_DOMINATED_BY)) {
                    iter_testMlooser.setInLoosers(true);
                    iter_testMlooser.setNewInLoosers(true);
                    failed = true;
                }
            }
            // new iterator for compare, skipp small_t
            iter_compare = (LinkedPointListIterator) datalist.iterator();
            // loop compare
            compare_loop:
            while (true) {
                // break if there is no next small c
                float[] small_c = null;

                small_c = iter_compare.nextInTestButNotThisAndLooser(iter_testMlooser.getDataNode());
                // exit of C is empty
                if (small_c == null) {
                    break compare_loop;
                }
                //
                PointRelationship compareresult = comparator.compare(small_t, small_c);
                cpuCost++;
                if (compareresult.equals(PointRelationship.DOMINATES)) {
                    iter_compare.setInLoosers(true);
                    iter_compare.setNewInLoosers(true);
                }
                if (compareresult.equals(PointRelationship.IS_DOMINATED_BY)) {
                    iter_testMlooser.setInLoosers(true);
                    iter_testMlooser.setNewInLoosers(true);
                    failed = true;
                    break compare_loop;
                }
            }
            if (!failed) {
                iter_testMlooser.setInSkyline(true);
            }
            iter_testMlooser.setInTest(false);
        }

        //
        if (profiler != null) {
            BNLProfiler.updateProfiler(profiler, insertions, deletions, moves, drops, cpuCost);
        }


        this.totalTimeNS = System.nanoTime() - startTime;

        // remove all non-skyline items
        LinkedPointListIterator iter = (LinkedPointListIterator) datalist.iterator();
        while (iter.hasNext()) {
            iter.nextDirect();
            if (!iter.isInSkyline()) {
                iter.remove();
            }
        }

        //
        return datalist;
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
        buf.append("RDF nm-dominance SEMI-NAIVE " + comparator.toString());
        return buf.toString();
    }

    @Override
    public String getShortName() {
        return "RDF_NM_SN";
    }
}
