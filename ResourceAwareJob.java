//import java.util.*;

/**
 * Resource-aware job class that includes memory and network requirements
 */
public class ResourceAwareJob extends DependencyAwareJob {
    // Resource requirements
    private int memoryRequirement;   // in MB
    private int networkRequirement;  // in Mbps
    private int cpuRequirement;      // percentage of CPU cores (1-100 per core)
    
    // Resource availability flags
    private boolean memoryConstrained;
    private boolean networkConstrained;
    private boolean cpuConstrained;
    
    /**
     * Create a new ResourceAwareJob
     */
    public ResourceAwareJob(int id, int arrivalTime, int executionTime, int priority) {
        super(id, arrivalTime, executionTime, priority);
        
        // Default resource requirements
        this.memoryRequirement = 1024;  // 1 GB default
        this.networkRequirement = 100;  // 100 Mbps default
        this.cpuRequirement = 100;      // 1 core default
        
        this.memoryConstrained = false;
        this.networkConstrained = false;
        this.cpuConstrained = false;
    }
    
    /**
     * Copy constructor
     */
    public ResourceAwareJob(ResourceAwareJob other) {
        super(other);
        this.memoryRequirement = other.memoryRequirement;
        this.networkRequirement = other.networkRequirement;
        this.cpuRequirement = other.cpuRequirement;
        this.memoryConstrained = other.memoryConstrained;
        this.networkConstrained = other.networkConstrained;
        this.cpuConstrained = other.cpuConstrained;
    }
    
    /**
     * Set resource requirements
     */
    public void setResourceRequirements(int memory, int network, int cpu) {
        this.memoryRequirement = memory;
        this.networkRequirement = network;
        this.cpuRequirement = cpu;
    }
    
    /**
     * Get memory requirement in MB
     */
    public int getMemoryRequirement() {
        return memoryRequirement;
    }
    
    /**
     * Get network requirement in Mbps
     */
    public int getNetworkRequirement() {
        return networkRequirement;
    }
    
    /**
     * Get CPU requirement (percentage)
     */
    public int getCpuRequirement() {
        return cpuRequirement;
    }
    
    /**
     * Set resource constraint flags
     */
    public void setResourceConstraints(boolean memory, boolean network, boolean cpu) {
        this.memoryConstrained = memory;
        this.networkConstrained = network;
        this.cpuConstrained = cpu;
    }
    
    /**
     * Check if job is memory constrained
     */
    public boolean isMemoryConstrained() {
        return memoryConstrained;
    }
    
    /**
     * Check if job is network constrained
     */
    public boolean isNetworkConstrained() {
        return networkConstrained;
    }
    
    /**
     * Check if job is CPU constrained
     */
    public boolean isCpuConstrained() {
        return cpuConstrained;
    }
    
    /**
     * Check if job is resource constrained in any way
     */
    public boolean isResourceConstrained() {
        return memoryConstrained || networkConstrained || cpuConstrained;
    }
    
    /**
     * Get an estimate of how resource constraints affect execution time
     * 
     * @return Factor to multiply execution time by (>= 1.0)
     */
    public double getResourceConstraintFactor() {
        double factor = 1.0;
        
        if (memoryConstrained) {
            factor *= 1.5; // Memory constraints slow execution by 50%
        }
        
        if (networkConstrained) {
            factor *= 1.3; // Network constraints slow execution by 30%
        }
        
        if (cpuConstrained) {
            factor *= 2.0; // CPU constraints slow execution by 100%
        }
        
        return factor;
    }
    
    @Override
    public String toString() {
        return String.format("ResourceJob #%d (Arrival: %d, Exec: %d, Priority: %d, Mem: %d, Net: %d, CPU: %d)",
                           getId(), getArrivalTime(), getExecutionTime(), getPriority(), 
                           memoryRequirement, networkRequirement, cpuRequirement);
    }
}