

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SkylineAlgorithmPPackageSync extends SkylineAlgorithmAbstractBarrierSync {
 
    public static int packageSize = 2000;
    private PointSourcePackageQueue pointListQueue;

    /**
     * Constructor.
     */
    public SkylineAlgorithmPPackageSync() {
    }

    /**
     * Creates all necesarry skyline workers and their repective threads.
     * @param numOfWorkers number of workers to create
     * @param initialDataQueue the data queue wrapping the point source
     */
    @Override
    protected void setupWorkers(int numOfWorkers, PointSource data) {
        // setup data source
        pointListQueue = new PointSourcePackageQueue(data);
        pointListQueue.setObjectToPetrify(Math.max(2, objectsToPetrify / packageSize));
        //
        workers = new ArrayList<SkylineWorker>();
        threads = new ArrayList<Thread>();
        //
	int d = data.getD();
	//
        BlockingQueue<DataPackage> inQueue = pointListQueue;
        BlockingQueue<DataPackage> outQueue;
        for (int i = 0; i < numOfWorkers; i++) {
            if (i == numOfWorkers - 1) {
                outQueue = null;
            } else {
                outQueue = new LinkedBlockingDeque<DataPackage>(queueCapacity);
            }
            SkylineWorkerPackageSync worker = new SkylineWorkerPackageSync(d, inQueue, outQueue, this);
            //          SkylineWorker worker = new SkylineWorker(inQueue, outQueue, this);
            worker.setId(i);
            workers.add(worker);
            threads.add(new Thread(worker, "Worker-" + i));
            inQueue = outQueue;
        }
    }

    @Override
    public synchronized List<float[]> compute(PointSource data) {
        long startTime = System.nanoTime();


        // create new worker threads and reorganizer
        this.setupWorkers(numOfWorkers, data);
        try {
            reorganizer = (Reorganizer) reorganizerClass.newInstance();
        //  System.out.println("Using Reorganizer: " + reorganizer);

        } catch (Exception ex) {
            Logger.getLogger(SkylineAlgorithmPQueueSync.class.getName()).log(Level.SEVERE, null, ex);
        }
        reorganizer.setWorkers(workers);

        // setup thread synchronization
        barrier = new CyclicBarrier(numOfWorkers, reorganizer);

        // run!
        for (Thread thread : threads) {
            thread.start();
        }

        // wait
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(SkylineAlgorithmPQueueSync.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // TODO: Join lists to form result list
        int size = 0;
        for (SkylineWorker worker : workers) {
            size += worker.getWindow().size();
        }

        totalTimeNS = System.nanoTime() - startTime;

        return Arrays.asList(new float[size][0]);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Parallel_Package#").append(packageSize).append("#W");
        buf.append(numOfWorkers);
        buf.append(" (reorganizer: " + reorganizerClass.getSimpleName() + ")");
        return buf.toString();
    }

    @Override
    public String getShortName() {
         StringBuffer sb = new StringBuffer("Package(");
        sb.append(numOfWorkers);
        return sb.append(")").toString();
    }
}
