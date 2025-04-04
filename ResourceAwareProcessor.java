import java.util.*;

/**
 * Resource-aware processor that tracks available resources
 */
public class ResourceAwareProcessor extends Processor {
    // Total resource capacities
    private int totalMemoryCapacity;    // in MB
    private int totalNetworkCapacity;   // in Mbps
    private int totalCpuCapacity;       // percentage (100 per core)
    
    // Available resources (changes as jobs are assigned)
    private int availableMemory;
    private int availableNetwork;
    private int availableCpu;
    
    // Resource usage history
    private List<ResourceUsageSnapshot> resourceHistory;
    
    /**
     * Create a resource-aware processor
     */
    public ResourceAwareProcessor(int id, double speedFactor, 
                                 int memoryCapacity, int networkCapacity, int cpuCapacity) {
        super(id, speedFactor);
        this.totalMemoryCapacity = memoryCapacity;
        this.totalNetworkCapacity = networkCapacity;
        this.totalCpuCapacity = cpuCapacity;
        
        // Initially all resources are available
        this.availableMemory = memoryCapacity;
        this.availableNetwork = networkCapacity;
        this.availableCpu = cpuCapacity;
        
        this.resourceHistory = new ArrayList<>();
        
        // Record initial state
        recordResourceUsage(0);
    }
    
    /**
     * Copy constructor
     */
    public ResourceAwareProcessor(ResourceAwareProcessor other) {
        super(other);
        this.totalMemoryCapacity = other.totalMemoryCapacity;
        this.totalNetworkCapacity = other.totalNetworkCapacity;
        this.totalCpuCapacity = other.totalCpuCapacity;
        this.availableMemory = other.availableMemory;
        this.availableNetwork = other.availableNetwork;
        this.availableCpu = other.availableCpu;
        this.resourceHistory = new ArrayList<>(other.resourceHistory);
    }
    
    /**
     * Get total memory capacity
     */
    public int getTotalMemoryCapacity() {
        return totalMemoryCapacity;
    }
    
    /**
     * Get total network capacity
     */
    public int getTotalNetworkCapacity() {
        return totalNetworkCapacity;
    }
    
    /**
     * Get total CPU capacity
     */
    public int getTotalCpuCapacity() {
        return totalCpuCapacity;
    }
    
    /**
     * Get available memory
     */
    public int getAvailableMemory() {
        return availableMemory;
    }
    
    /**
     * Get available network
     */
    public int getAvailableNetwork() {
        return availableNetwork;
    }
    
    /**
     * Get available CPU
     */
    public int getAvailableCpu() {
        return availableCpu;
    }
    
    /**
     * Check if the processor can accommodate a resource-aware job
     */
    public boolean canAccommodate(ResourceAwareJob job) {
        return availableMemory >= job.getMemoryRequirement() &&
               availableNetwork >= job.getNetworkRequirement() &&
               availableCpu >= job.getCpuRequirement();
    }
    
    /**
     * Assign a job to this processor with resource allocation
     */
    @Override
    public void assignJob(Job job, int currentTime) {
        if (job instanceof ResourceAwareJob) {
            ResourceAwareJob resourceJob = (ResourceAwareJob) job;
            
            // Check if we can accommodate this job
            if (!canAccommodate(resourceJob)) {
                // Set constraint flags on the job
                boolean memoryConstrained = availableMemory < resourceJob.getMemoryRequirement();
                boolean networkConstrained = availableNetwork < resourceJob.getNetworkRequirement();
                boolean cpuConstrained = availableCpu < resourceJob.getCpuRequirement();
                
                resourceJob.setResourceConstraints(memoryConstrained, networkConstrained, cpuConstrained);
                
                // Adjust execution time based on constraints
                double constraintFactor = resourceJob.getResourceConstraintFactor();
                int adjustedExecTime = (int) Math.ceil(resourceJob.getExecutionTime() * constraintFactor);
                
                // Create a temporary job with adjusted execution time
                Job tempJob = new Job(resourceJob.getId(), resourceJob.getArrivalTime(), 
                                     adjustedExecTime, resourceJob.getPriority());
                
                // Assign the temporary job
                super.assignJob(tempJob, currentTime);
                return;
            }
            
            // Allocate resources
            availableMemory -= resourceJob.getMemoryRequirement();
            availableNetwork -= resourceJob.getNetworkRequirement();
            availableCpu -= resourceJob.getCpuRequirement();
        }
        
        // Call parent method to handle basic assignment
        super.assignJob(job, currentTime);
        
        // Record resource usage
        recordResourceUsage(currentTime);
    }
    
    /**
     * Complete a job and release its resources
     */
    @Override
    public void completeJob(int currentTime) {
        Job completedJob = getCurrentJob();
        
        if (completedJob != null && completedJob instanceof ResourceAwareJob) {
            ResourceAwareJob resourceJob = (ResourceAwareJob) completedJob;
            
            // Release resources if job was not constrained
            if (!resourceJob.isResourceConstrained()) {
                availableMemory += resourceJob.getMemoryRequirement();
                availableNetwork += resourceJob.getNetworkRequirement();
                availableCpu += resourceJob.getCpuRequirement();
                
                // Cap at maximum capacity
                availableMemory = Math.min(availableMemory, totalMemoryCapacity);
                availableNetwork = Math.min(availableNetwork, totalNetworkCapacity);
                availableCpu = Math.min(availableCpu, totalCpuCapacity);
            }
        }
        
        // Call parent method to handle basic completion
        super.completeJob(currentTime);
        
        // Record resource usage
        recordResourceUsage(currentTime);
    }
    
    /**
     * Record current resource usage for analytics
     */
    private void recordResourceUsage(int currentTime) {
        ResourceUsageSnapshot snapshot = new ResourceUsageSnapshot(
            currentTime,
            totalMemoryCapacity - availableMemory,
            totalNetworkCapacity - availableNetwork,
            totalCpuCapacity - availableCpu
        );
        
        resourceHistory.add(snapshot);
    }
    
    /**
     * Get resource utilization history
     */
    public List<ResourceUsageSnapshot> getResourceHistory() {
        return Collections.unmodifiableList(resourceHistory);
    }
    
    /**
     * Get average resource utilization
     */
    public ResourceUtilization getAverageResourceUtilization() {
        if (resourceHistory.isEmpty()) {
            return new ResourceUtilization(0, 0, 0);
        }
        
        double totalMemoryUsage = 0;
        double totalNetworkUsage = 0;
        double totalCpuUsage = 0;
        
        for (ResourceUsageSnapshot snapshot : resourceHistory) {
            totalMemoryUsage += snapshot.getMemoryUsage();
            totalNetworkUsage += snapshot.getNetworkUsage();
            totalCpuUsage += snapshot.getCpuUsage();
        }
        
        int count = resourceHistory.size();
        
        return new ResourceUtilization(
            totalMemoryUsage / count / totalMemoryCapacity,
            totalNetworkUsage / count / totalNetworkCapacity,
            totalCpuUsage / count / totalCpuCapacity
        );
    }
    
    @Override
    public String toString() {
        return String.format("ResourceProcessor #%d (Speed: %.1f, Mem: %d, Net: %d, CPU: %d)",
                           getId(), getSpeedFactor(), totalMemoryCapacity, 
                           totalNetworkCapacity, totalCpuCapacity);
    }
}