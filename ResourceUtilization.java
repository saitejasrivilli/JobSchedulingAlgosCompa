/**
 * Class to store overall resource utilization percentages
 */
public class ResourceUtilization {
    private double memoryUtilization;
    private double networkUtilization;
    private double cpuUtilization;
    
    public ResourceUtilization(double memoryUtilization, double networkUtilization, double cpuUtilization) {
        this.memoryUtilization = memoryUtilization;
        this.networkUtilization = networkUtilization;
        this.cpuUtilization = cpuUtilization;
    }
    
    public double getMemoryUtilization() {
        return memoryUtilization;
    }
    
    public double getNetworkUtilization() {
        return networkUtilization;
    }
    
    public double getCpuUtilization() {
        return cpuUtilization;
    }
    
    /**
     * Get overall utilization as average of all resources
     */
    public double getOverallUtilization() {
        return (memoryUtilization + networkUtilization + cpuUtilization) / 3.0;
    }
}