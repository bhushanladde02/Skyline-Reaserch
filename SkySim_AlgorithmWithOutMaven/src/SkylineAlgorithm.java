

import java.util.List;

public interface SkylineAlgorithm {

    public List<float[]> compute(PointSource data);

    public long getIOcost();

    public long getCPUcost();

    public long getNumberOfNodesVisited();

    public long getTotalTimeNS();

    public long getReorgTimeNS();

    public long getPreprocessTimeNS();

    public long getMemoryConsumptionInBytes();

    public String getShortName();

    public SimulatorConfiguration getExperimentConfig();

    public void setExperimentConfig(SimulatorConfiguration config);
}