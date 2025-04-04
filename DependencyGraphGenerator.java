import java.util.*;

/**
 * Utility class for generating job dependency graphs
 */
public class DependencyGraphGenerator {
    private Random random;
    
    public DependencyGraphGenerator(long seed) {
        this.random = new Random(seed);
        }
    
    
    /**
     * Generate jobs with dependencies
     * 
     * @param count Number of jobs to generate
     * @param maxDependencies Maximum dependencies per job
     * @param dagWidth Maximum width of dependency graph
     * @return List of jobs with dependencies
     */
    public List<DependencyAwareJob> generateJobsWithDependencies(int count, int maxDependencies, int dagWidth) {
        List<DependencyAwareJob> jobs = new ArrayList<>();
        
        // Create jobs first
        for (int i = 0; i < count; i++) {
            int arrivalTime = random.nextInt(count / 3); // Distribute arrivals
            int executionTime = 1 + random.nextInt(20);
            int priority = random.nextInt(10) + 1;
            
            DependencyAwareJob job = new DependencyAwareJob(i, arrivalTime, executionTime, priority);
            jobs.add(job);
        }
        
        // Add dependencies (layered approach for DAG generation)
        int numLayers = Math.max(2, count / dagWidth);
        int jobsPerLayer = count / numLayers;
        
        for (int layer = 1; layer < numLayers; layer++) {
            int layerStart = layer * jobsPerLayer;
            int layerEnd = Math.min(count, (layer + 1) * jobsPerLayer);
            
            for (int jobIndex = layerStart; jobIndex < layerEnd; jobIndex++) {
                DependencyAwareJob job = jobs.get(jobIndex);
                
                // Add dependencies to previous layer
                int numDeps = 1 + random.nextInt(Math.min(maxDependencies, jobsPerLayer));
                int prevLayerStart = (layer - 1) * jobsPerLayer;
                int prevLayerEnd = layerStart;
                
                for (int d = 0; d < numDeps; d++) {
                    int depIndex = prevLayerStart + random.nextInt(prevLayerEnd - prevLayerStart);
                    DependencyAwareJob depJob = jobs.get(depIndex);
                    
                    // Assign random dependency type with higher probability for REQUIRES
                    double typeRoll = random.nextDouble();
                    DependencyAwareJob.DependencyType type;
                    
                    if (typeRoll < 0.7) {
                        type = DependencyAwareJob.DependencyType.REQUIRES;
                    } else if (typeRoll < 0.9) {
                        type = DependencyAwareJob.DependencyType.PREFERS;
                    } else {
                        type = DependencyAwareJob.DependencyType.CONFLICTS_WITH;
                    }
                    
                    job.addDependency(depJob, type);
                }
            }
        }
        
        // Calculate critical paths
        for (DependencyAwareJob job : jobs) {
            job.calculateCriticalPath();
        }
        
        return jobs;
    }
    
    /**
     * Generate jobs with specific dependency patterns
     * 
     * @param pattern Type of dependency pattern to generate
     * @return List of jobs with dependencies
     */
    public List<DependencyAwareJob> generatePatternedDependencies(String pattern, int count) {
        switch (pattern.toLowerCase()) {
            case "linear":
                return generateLinearDependencies(count);
            case "tree":
                return generateTreeDependencies(count);
            case "diamond":
                return generateDiamondDependencies(count);
            case "pipeline":
                return generatePipelineDependencies(count);
            default:
                return generateJobsWithDependencies(count, 3, 5);
        }
    }
    
    /**
     * Generate a linear chain of dependencies (A -> B -> C -> ...)
     */
    private List<DependencyAwareJob> generateLinearDependencies(int count) {
        List<DependencyAwareJob> jobs = new ArrayList<>();
        
        // Create jobs first
        for (int i = 0; i < count; i++) {
            int arrivalTime = 0; // All jobs available at start
            int executionTime = 1 + random.nextInt(10);
            int priority = random.nextInt(10) + 1;
            
            DependencyAwareJob job = new DependencyAwareJob(i, arrivalTime, executionTime, priority);
            jobs.add(job);
        }
        
        // Create linear dependencies (each job depends on previous job)
        for (int i = 1; i < count; i++) {
            jobs.get(i).addDependency(jobs.get(i-1), DependencyAwareJob.DependencyType.REQUIRES);
        }
        
        // Calculate critical paths
        for (DependencyAwareJob job : jobs) {
            job.calculateCriticalPath();
        }
        
        return jobs;
    }
    
    /**
     * Generate a tree of dependencies
     */
    private List<DependencyAwareJob> generateTreeDependencies(int count) {
        List<DependencyAwareJob> jobs = new ArrayList<>();
        
        // Create jobs first
        for (int i = 0; i < count; i++) {
            int arrivalTime = 0; // All jobs available at start
            int executionTime = 1 + random.nextInt(10);
            int priority = random.nextInt(10) + 1;
            
            DependencyAwareJob job = new DependencyAwareJob(i, arrivalTime, executionTime, priority);
            jobs.add(job);
        }
        
        // Create tree dependencies
        for (int i = 1; i < count; i++) {
            int parentIndex = (i - 1) ;
        }
        // Create tree dependencies
    for (int i = 1; i < count; i++) {
        int parentIndex = (i - 1) / 2; // Binary tree parent formula
        jobs.get(i).addDependency(jobs.get(parentIndex), DependencyAwareJob.DependencyType.REQUIRES);
    }
    
    // Calculate critical paths
    for (DependencyAwareJob job : jobs) {
        job.calculateCriticalPath();
    }
    
    return jobs;
        }

/**
 * Generate a diamond pattern of dependencies
 */
private List<DependencyAwareJob> generateDiamondDependencies(int count) {
    if (count < 4) {
        throw new IllegalArgumentException("Diamond pattern requires at least 4 jobs");
    }
    
    List<DependencyAwareJob> jobs = new ArrayList<>();
    
    // Create jobs first
    for (int i = 0; i < count; i++) {
        int arrivalTime = 0; // All jobs available at start
        int executionTime = 1 + random.nextInt(10);
        int priority = random.nextInt(10) + 1;
        
        DependencyAwareJob job = new DependencyAwareJob(i, arrivalTime, executionTime, priority);
        jobs.add(job);
    }
    
    // Create diamond pattern:
    // - First job is the entry point
    // - Middle jobs depend on first job 
    // - Last job depends on all middle jobs
    
    // First job is entry point (no dependencies)
    DependencyAwareJob entryJob = jobs.get(0);
    DependencyAwareJob exitJob = jobs.get(count - 1);
    
    // Middle jobs depend on first job
    for (int i = 1; i < count - 1; i++) {
        jobs.get(i).addDependency(entryJob, DependencyAwareJob.DependencyType.REQUIRES);
        exitJob.addDependency(jobs.get(i), DependencyAwareJob.DependencyType.REQUIRES);
    }
    
    // Calculate critical paths
    for (DependencyAwareJob job : jobs) {
        job.calculateCriticalPath();
    }
    
    return jobs;
}

/**
 * Generate a pipeline pattern of dependencies (several parallel chains)
 */
private List<DependencyAwareJob> generatePipelineDependencies(int count) {
    int pipelineStages = 5;
    int pipelines = Math.max(1, count / pipelineStages);
    
    List<DependencyAwareJob> jobs = new ArrayList<>();
    
    // Create jobs first
    for (int i = 0; i < count; i++) {
        int arrivalTime = 0; // All jobs available at start
        int executionTime = 1 + random.nextInt(10);
        int priority = random.nextInt(10) + 1;
        
        DependencyAwareJob job = new DependencyAwareJob(i, arrivalTime, executionTime, priority);
        jobs.add(job);
    }
    
    // Create pipeline dependencies
    for (int p = 0; p < pipelines; p++) {
        for (int stage = 1; stage < pipelineStages; stage++) {
            int jobIndex = p * pipelineStages + stage;
            int prevIndex = p * pipelineStages + stage - 1;
            
            if (jobIndex < count && prevIndex < count) {
                jobs.get(jobIndex).addDependency(jobs.get(prevIndex), DependencyAwareJob.DependencyType.REQUIRES);
            }
        }
    }
    
    // Calculate critical paths
    for (DependencyAwareJob job : jobs) {
        job.calculateCriticalPath();
    }
    
    return jobs;
}
}