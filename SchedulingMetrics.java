/**
 * Class to store scheduling performance metrics
 */
public class SchedulingMetrics {
    private double makespan;
    private double averageWaitingTime;
    private double averageTurnaroundTime;
    private double throughput;
    private double resourceUtilization;
    
    public SchedulingMetrics(double makespan, double averageWaitingTime, double averageTurnaroundTime, 
                           double throughput, double resourceUtilization) {
        this.makespan = makespan;
        this.averageWaitingTime = averageWaitingTime;
        this.averageTurnaroundTime = averageTurnaroundTime;
        this.throughput = throughput;
        this.resourceUtilization = resourceUtilization;
    }
    
    public double getMakespan() {
        return makespan;
    }
    
    public double getAverageWaitingTime() {
        return averageWaitingTime;
    }
    
    public double getAverageTurnaroundTime() {
        return averageTurnaroundTime;
    }
    
    public double getThroughput() {
        return throughput;
    }
    
    public double getResourceUtilization() {
        return resourceUtilization;
    }
}