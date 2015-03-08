

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Simulator {

    public static String getInfo(SimulatorConfiguration config) {
	StringBuffer result = new StringBuffer();
	result.append(String.format("    Algorithm: %s%n", config.getSkylineAlgorithm()));
	result.append(String.format("DataGenerator: %s%n", config.getDataGenerator().getShortName()));
	result.append(String.format("   DataSorter: %s%n", config.getPresortStrategy()));
	result.append(String.format("      Runtime: %s %s%n", System.getProperty("java.runtime.name"), System.getProperty("java.runtime.version")));
	String hostname = System.getenv("HOSTNAME"); // UNIX
	if (hostname == null) {
	    hostname = System.getenv("COMPUTERNAME"); // Windows
	}
	result.append(String.format("         Host: %s%n", hostname));
	result.append(String.format("       Trials: %s", config.getNumTrials()));
	return result.toString();
    }

    public static String getGnuplotInfo(SimulatorConfiguration config) {
	String info = getInfo(config);
	return preprendEveryLineWith(info, "# ");
    }

    private static String preprendEveryLineWith(String in, String prefix) {
	StringBuffer result = new StringBuffer();
	BufferedReader reader = new BufferedReader(new StringReader(in));
	String line;
	try {
	    while ((line = reader.readLine()) != null) {
		result.append(String.format("# %s%n", line));
	    }
	} catch (IOException ex) {
	    Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
	}
	return result.toString();
    }

    public Reporter run(SimulatorConfiguration config) {
	return run(config, true);
    }

    public Reporter run(SimulatorConfiguration config, boolean printOut) {
        if (config == null) {
            throw new IllegalArgumentException("No configuration set.");
        }


	// Setup features
	List<Feature> fs = new ArrayList<Feature>();

	Feature featD = new LongFeature("d", 2);
	Feature featN = new LongFeature("n", 9);
	Feature featSky = new LongFeature("skysize", 9);
	Feature featTimeGenMS = new DoubleFeature("time: gen", 9, 1, " ms");
	Feature featTimeSortMS = new DoubleFeature("time: sort", 10, 1, " ms");
	Feature featTimePreMS = new DoubleFeature("time: pre", 9, 1, " ms");
	Feature featTimeCompMS = new DoubleFeature("time: comp", 10, 1, " ms");
	Feature featTimeReorgMS = new DoubleFeature("time: reorg", 11, 1, " ms");
	Feature featOps = new LongFeature("#ops", 10);
	Feature featOpsPerPoint = new DoubleFeature("#ops/n", 9, 1);
	Feature featNodes = new LongFeature("#nodesV", 10);
	Feature featNodesPerPoint = new DoubleFeature("#nodesV/n", 9, 1);
	Feature featTimeCompMSPerOp = new DoubleFeature("time/#ops", 9, 1, " ns");
	Feature featMemoryMB = new DoubleFeature("mem", 9, 1, " MB");

	fs.add(featD);
	fs.add(featN);
	fs.add(featSky);
	fs.add(featTimeGenMS);
	fs.add(featTimeSortMS);
//	fs.add(featTimePreMS);
	fs.add(featTimeCompMS);
//	fs.add(featTimeReorgMS);
	fs.add(featOps);
	fs.add(featOpsPerPoint);
	fs.add(featTimeCompMSPerOp);
	fs.add(featNodes);
	fs.add(featNodesPerPoint);
	fs.add(featMemoryMB);


        DataGenerator dg = config.getDataGenerator();
        if (config.isUseDefaultGeneratorSeed()) {
            dg.resetToDefaultSeed();
        }
        SkylineAlgorithm alg = null;
        alg = config.getSkylineAlgorithm();
        alg.setExperimentConfig(config);

	if (printOut) {
	    System.out.println();
	    System.out.println(getInfo(config));
	    System.out.println();
	}
	
	Reporter r = new Reporter(alg.getShortName(), config.getNumTrials(), fs);

	if (printOut) {
	    System.out.print(r.getHeader());
	    System.out.print(r.getDivider());
	}

        int numTrials = config.getNumTrials();
        int d = config.getD();
        int n = config.getN();
        int[] numLevels = config.getNumLevels();
        DataSource ds = config.getDataSource();
        File df = config.getDataFile();
        int bytesPerRecord = config.getBytesPerRecord();
        SimulatorConfiguration.PresortStrategy presortStrategy = config.getPresortStrategy();

        // Start with the calibration trial,
        // then with all "real" trials
        for (int i = -1; i < numTrials; i++) {
	    Map<Feature, Object> trialReport = new HashMap<Feature, Object>();
            long tStart, tStop;
	    
            float[] data = null;
            tStart = System.nanoTime();
            if (ds == DataSource.MEMORY) {
                data = dg.generate(d, n, numLevels);
            } else {
                try {
                    if (ds == DataSource.FILE) {
                        dg.generate(d, n, numLevels, df, bytesPerRecord);
                    }
                    // clear file system cache
                    System.out.print("clearing cache ... ");
                    String[] commands = {"/bin/bash"};
                    ProcessBuilder pb = new ProcessBuilder(commands);
                    Process proc = pb.start();
                    Writer outCommand = new OutputStreamWriter(proc.getOutputStream());
                    outCommand.write("sudo sh -c 'echo 3 >/proc/sys/vm/drop_caches'; exit\n");
                    outCommand.flush();
                    proc.waitFor();
                    outCommand.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            tStop = System.nanoTime();
	    trialReport.put(featTimeGenMS, (tStop - tStart) * 1e-6);

            PointSource pointSource;
            if (ds == DataSource.MEMORY) {
                pointSource = new PointSourceRAM(d, data);
            } else {
                pointSource = new PointSourceDisk(df, bytesPerRecord, n, d);
            }
            tStart = System.nanoTime();
            switch (presortStrategy) {
                case FullPresortVolume:
                    pointSource = DataPresorter.sortByVolume(pointSource);
                    break;
		case FullPresortSum:
                    pointSource = DataPresorter.sortBySum(pointSource);
                    break;
		case FullPresortZAddress:
		    pointSource = DataPresorter.sortByZAddress(pointSource);
		    break;
                case PartialPresort:
                    pointSource = DataPresorter.sortBestToFront(pointSource, config.getPartialPresortThreshold());
                    break;
                case NoPresort:
                default:
                    break;
            }
            tStop = System.nanoTime();
	    trialReport.put(featTimeSortMS, (tStop - tStart) * 1e-6);

	    // Clean memory (for measuring memory consumption)
	    Runtime.getRuntime().gc();
	    long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            List<float[]> window = alg.compute(pointSource);
         Iterator itr=window.iterator();
         while(itr.hasNext())
         {
        	 float[] a=(float[]) itr.next();
        	 System.out.println(a[0]+" and "+a[1]);
         }
         
	    long memAfter = alg.getMemoryConsumptionInBytes();
	    trialReport.put(featMemoryMB, (memAfter - memBefore) * 1e-6);
	    trialReport.put(featTimeCompMS, alg.getTotalTimeNS() * 1e-6);
	    trialReport.put(featTimePreMS, alg.getPreprocessTimeNS() * 1e-6);
	    trialReport.put(featTimeReorgMS, alg.getReorgTimeNS() * 1e-6);
	    trialReport.put(featOps, alg.getCPUcost());
	    trialReport.put(featOpsPerPoint, (double)alg.getCPUcost() / n);
	    trialReport.put(featTimeCompMSPerOp, (double)alg.getTotalTimeNS() / alg.getCPUcost());
	    trialReport.put(featNodes, alg.getNumberOfNodesVisited());
	    trialReport.put(featNodesPerPoint, (double)alg.getNumberOfNodesVisited() / n);
	    trialReport.put(featD, new Long(d));
	    trialReport.put(featN, new Long(n));
	    trialReport.put(featSky, new Long(window.size()));

	    if (i > -1) {
		String reportStr = r.reportTrial(trialReport);
		if (printOut) {
		    System.out.print(reportStr);
		}
	    } else {
		String reportStr = r.reportCalibration(trialReport);
		if (printOut) {
		    System.out.print(reportStr);
		    System.out.print(r.getDivider());
		}
	    }
        }

	if (printOut) {
	    System.out.print(r.getDivider());
	    System.out.print(r.getAverages());
	    System.out.print(r.getDivider());
	    System.out.print(r.getHeader());
	}
	
	return r;
    }
}
