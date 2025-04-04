import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Job class with dependency management
 */
public class DependencyAwareJob extends Job {
    // Dependency relationship types
    public enum DependencyType {
        REQUIRES,        // Job requires all dependencies to complete first
        PREFERS,         // Job runs better if dependencies complete first
        CONFLICTS_WITH   // Job cannot run at same time as dependencies
    }
    
    // Map to track dependencies and their types
    private Map<Job, DependencyType> dependencies;
    
    // Jobs that depend on this job
    private Set<DependencyAwareJob> dependents;
    
    // Critical path length (longest path of dependencies)
    private int criticalPathLength;
    
    // Earliest possible start time based on dependencies
    private int earliestStartTime;
    
    /**
     * Create a new DependencyAwareJob
     */
    public DependencyAwareJob(int id, int arrivalTime, int executionTime, int priority) {
        super(id, arrivalTime, executionTime, priority);
        this.dependencies = new HashMap<>();
        this.dependents = new HashSet<>();
        this.criticalPathLength = executionTime;
        this.earliestStartTime = arrivalTime;
    }
    
    /**
     * Copy constructor
     */
    public DependencyAwareJob(DependencyAwareJob other) {
        super(other);
        this.dependencies = new HashMap<>(other.dependencies);
        this.dependents = new HashSet<>(other.dependents);
        this.criticalPathLength = other.criticalPathLength;
        this.earliestStartTime = other.earliestStartTime;
    }
    
    /**
     * Add a dependency relationship
     * 
     * @param job The job this job depends on
     * @param type Type of dependency relationship
     */
    public void addDependency(Job job, DependencyType type) {
        dependencies.put(job, type);
        
        // Add this job as a dependent to the dependency
        if (job instanceof DependencyAwareJob) {
            ((DependencyAwareJob) job).addDependent(this);
        }
        
        // Recalculate critical path
        calculateCriticalPath();
    }
    
    /**
     * Add a dependent job (a job that depends on this one)
     * 
     * @param job The job that depends on this one
     */
    public void addDependent(DependencyAwareJob job) {
        dependents.add(job);
    }
    
    /**
     * Get all dependencies of this job
     * 
     * @return Map of dependency jobs and their relationship types
     */
    public Map<Job, DependencyType> getDependencyMap() {
        return Collections.unmodifiableMap(dependencies);
    }
    
    /**
     * Get dependencies of a specific type
     * 
     * @param type Type of dependency relationship
     * @return List of jobs with the specified dependency type
     */
    public List<Job> getDependenciesByType(DependencyType type) {
        return dependencies.entrySet().stream()
                         .filter(entry -> entry.getValue() == type)
                         .map(Map.Entry::getKey)
                         .collect(Collectors.toList());
    }
    
    /**
     * Get all required dependencies (REQUIRES type)
     * 
     * @return List of required dependencies
     */
    public List<Job> getRequiredDependencies() {
        return getDependenciesByType(DependencyType.REQUIRES);
    }
    
    /**
     * Get jobs that depend on this job
     * 
     * @return Set of dependent jobs
     */
    public Set<DependencyAwareJob> getDependents() {
        return Collections.unmodifiableSet(dependents);
    }
    
    /**
     * Check if all required dependencies are met
     * 
     * @param completedJobs List of completed jobs
     * @return true if all required dependencies are completed
     */
    @Override
    public boolean areDependenciesMet(List<Job> completedJobs) {
        // Check that all REQUIRES dependencies are completed
        for (Map.Entry<Job, DependencyType> entry : dependencies.entrySet()) {
            if (entry.getValue() == DependencyType.REQUIRES && !completedJobs.contains(entry.getKey())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if this job conflicts with any currently running jobs
     * 
     * @param runningJobs List of currently running jobs
     * @return true if there are conflicts
     */
    public boolean hasConflicts(List<Job> runningJobs) {
        // Check for CONFLICTS_WITH dependencies
        for (Map.Entry<Job, DependencyType> entry : dependencies.entrySet()) {
            if (entry.getValue() == DependencyType.CONFLICTS_WITH && runningJobs.contains(entry.getKey())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Calculate the priority score including dependency information
     * 
     * @param completedJobs List of completed jobs
     * @return Enhanced priority score
     */
    public double calculatePriorityScore(List<Job> completedJobs) {
        double basePriority = getPriority();
        
        // Increase priority for jobs with many dependents (high fanout)
        double dependentFactor = 0.1 * dependents.size();
        
        // Increase priority for jobs on the critical path
        double criticalPathFactor = 0.2 * criticalPathLength;
        
        // Preferred dependencies bonus
        double preferredBonus = 0;
        for (Map.Entry<Job, DependencyType> entry : dependencies.entrySet()) {
            if (entry.getValue() == DependencyType.PREFERS && completedJobs.contains(entry.getKey())) {
                preferredBonus += 0.5;
            }
        }
        
        return basePriority + dependentFactor + criticalPathFactor + preferredBonus;
    }
    
    /**
     * Calculate the critical path length (longest path through dependency graph)
     */
    public void calculateCriticalPath() {
        // Base execution time
        int maxDependencyPath = 0;
        
        // Find the longest path through dependencies
        for (Job dependency : getRequiredDependencies()) {
            int dependencyPathLength = dependency.getExecutionTime();
            
            if (dependency instanceof DependencyAwareJob) {
                dependencyPathLength = ((DependencyAwareJob) dependency).getCriticalPathLength();
            }
            
            maxDependencyPath = Math.max(maxDependencyPath, dependencyPathLength);
        }
        
        // Critical path is longest dependency path plus this job's execution time
        this.criticalPathLength = maxDependencyPath + getExecutionTime();
        
        // Update earliest start time
        calculateEarliestStartTime();
        
        // Propagate the update to dependent jobs
        for (DependencyAwareJob dependent : dependents) {
            dependent.calculateCriticalPath();
        }
    }
    
    /**
     * Calculate the earliest possible start time based on dependencies
     */
    public void calculateEarliestStartTime() {
        // Start with job's arrival time
        int earliest = getArrivalTime();
        
        // Check all required dependencies
        for (Job dependency : getRequiredDependencies()) {
            // If dependency isn't complete, use its earliest completion time
            if (dependency.getCompletionTime() == -1) {
                int dependencyEst = dependency.getArrivalTime() + dependency.getExecutionTime();
                
                if (dependency instanceof DependencyAwareJob) {
                    DependencyAwareJob depJob = (DependencyAwareJob) dependency;
                    dependencyEst = depJob.getEarliestStartTime() + dependency.getExecutionTime();
                }
                
                earliest = Math.max(earliest, dependencyEst);
            } else {
                // If dependency is complete, use its actual completion time
                earliest = Math.max(earliest, dependency.getCompletionTime());
            }
        }
        
        this.earliestStartTime = earliest;
    }
    
    /**
     * Get the critical path length
     * 
     * @return Critical path length
     */
    public int getCriticalPathLength() {
        return criticalPathLength;
    }
    
    /**
     * Get the earliest possible start time
     * 
     * @return Earliest start time
     */
    public int getEarliestStartTime() {
        return earliestStartTime;
    }
    
    /**
     * Check if this job is a critical job (on the critical path with dependents)
     * 
     * @return true if this is a critical job
     */
    public boolean isCriticalJob() {
        return !dependents.isEmpty() && dependents.stream()
            .anyMatch(dep -> dep.getCriticalPathLength() - getExecutionTime() == getCriticalPathLength());
    }
    
    @Override
    public String toString() {
        return String.format("Job #%d (Arrival: %d, Exec: %d, Priority: %d, Dependencies: %d, Dependents: %d)",
                           getId(), getArrivalTime(), getExecutionTime(), getPriority(), 
                           dependencies.size(), dependents.size());
    }
} 