/**
 * Class to store resource usage at a point in time
 */
public class ResourceUsageSnapshot {
    private int timestamp;
    private int memoryUsage;
    private int networkUsage;
    private int cpuUsage;
    
    public ResourceUsageSnapshot(int timestamp, int memoryUsage, int networkUsage, int cpuUsage) {
        this.timestamp = timestamp;
        this.memoryUsage = memoryUsage;
        this.networkUsage = networkUsage;
        this.cpuUsage = cpuUsage;
    }
    
    public int getTimestamp() {
        return timestamp;
    }
    
    public int getMemoryUsage() {
        return memoryUsage;
    }
    
    public int getNetworkUsage() {
        return networkUsage;
    }
    
    public int getCpuUsage() {
        return cpuUsage;
    }
}