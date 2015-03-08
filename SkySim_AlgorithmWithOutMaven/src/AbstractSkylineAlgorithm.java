


public abstract class AbstractSkylineAlgorithm implements SkylineAlgorithm {

    protected SimulatorConfiguration config;
    protected long totalTimeNS;
    protected long cpuCost;
    protected long numberOfNodesVisited;
    protected long ioCost;
    protected long preprocessTimeNS;
    protected long reorgTimeNS;
    protected long memoryConsumption;

    @Override
    public SimulatorConfiguration getExperimentConfig() {
        return config;
    }

    @Override
    public void setExperimentConfig(SimulatorConfiguration config) {
        this.config = config;
    }

    @Override
    public long getIOcost() {
	return ioCost;
    }

    @Override
    public long getCPUcost() {
	return cpuCost;
    }

    @Override
    public long getNumberOfNodesVisited() {
	return numberOfNodesVisited;
    }

    @Override
    public long getTotalTimeNS() {
	return totalTimeNS;
    }

    @Override
    public long getReorgTimeNS() {
	return reorgTimeNS;
    }

    @Override
    public long getPreprocessTimeNS() {
	return preprocessTimeNS;
    }

    @Override
    public long getMemoryConsumptionInBytes() {
	return memoryConsumption;
    }
}
