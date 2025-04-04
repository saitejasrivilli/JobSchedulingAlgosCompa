public class JobHistoryRecord {
    private int jobId;
    private int estimatedTime;
    private int actualTime;
    private int priority;
    private boolean isIOBound;
    private int numDependencies;
    private int memoryRequirement;
    private int networkRequirement;
    
    public JobHistoryRecord(int jobId, int estimatedTime, int actualTime, int priority, 
                          boolean isIOBound, int numDependencies, 
                          int memoryRequirement, int networkRequirement) {
        this.jobId = jobId;
        this.estimatedTime = estimatedTime;
        this.actualTime = actualTime;
        this.priority = priority;
        this.isIOBound = isIOBound;
        this.numDependencies = numDependencies;
        this.memoryRequirement = memoryRequirement;
        this.networkRequirement = networkRequirement;
    }
    
    public int getJobId() {
        return jobId;
    }
    
    public int getEstimatedTime() {
        return estimatedTime;
    }
    
    public int getActualTime() {
        return actualTime;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public boolean isIOBound() {
        return isIOBound;
    }
    
    public int getNumDependencies() {
        return numDependencies;
    }
    
    public int getMemoryRequirement() {
        return memoryRequirement;
    }
    
    public int getNetworkRequirement() {
        return networkRequirement;
    }
 }