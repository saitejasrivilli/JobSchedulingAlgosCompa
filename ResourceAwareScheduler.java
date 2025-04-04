import java.util.*;
import java.util.stream.Collectors;

/**
 * Resource-aware scheduler that takes resource constraints into account
 */
public class ResourceAwareScheduler implements Scheduler {
    private String name;
    private Scheduler baseScheduler;
    
    /**
     * Create a resource-aware scheduler
     */
    public ResourceAwareScheduler(Scheduler baseScheduler) {
        this.baseScheduler = baseScheduler;
        this.name = "Resource-Aware " + baseScheduler.getName();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void scheduleJobs(List<Job> jobs, List<Processor> processors, int currentTime, List<Job> completedJobs) {
        // Filter jobs that are ready to run
        List<Job> availableJobs = jobs.stream()
                                    .filter(job -> job.getArrivalTime() <= currentTime && 
                                                  !job.isCompleted() && 
                                                  job.areDependenciesMet(completedJobs) &&
                                                  job.getStartTime() == -1)
                                    .collect(Collectors.toList());
        
        // Filter processors that are available
        List<Processor> availableProcessors = processors.stream()
                                                     .filter(p -> !p.isBusy(currentTime))
                                                     .collect(Collectors.toList());
        
        // Special handling for resource-aware processors and jobs
        if (!availableJobs.isEmpty() && !availableProcessors.isEmpty() && 
            availableJobs.get(0) instanceof ResourceAwareJob && 
            availableProcessors.get(0) instanceof ResourceAwareProcessor) {
            
            resourceAwareScheduling(
                availableJobs.stream().map(j -> (ResourceAwareJob)j).collect(Collectors.toList()),
                availableProcessors.stream().map(p -> (ResourceAwareProcessor)p).collect(Collectors.toList()),
                currentTime,
                completedJobs
            );
        } else {
            // Fall back to base scheduler if not all components are resource-aware
            baseScheduler.scheduleJobs(availableJobs, availableProcessors, currentTime, completedJobs);
        }
    }
    
    /**
     * Schedule resource-aware jobs on resource-aware processors
     */
    private void resourceAwareScheduling(List<ResourceAwareJob> jobs, List<ResourceAwareProcessor> processors, 
                                       int currentTime, List<Job> completedJobs) {
        if (jobs.isEmpty() || processors.isEmpty()) {
            return;
        }
        
        // Priority queue for jobs based on resource efficiency
        List<ResourceAwareJob> sortedJobs = new ArrayList<>(jobs);
        sortedJobs.sort((a, b) -> {
            // Calculate resource efficiency score (higher is better)
            double scoreA = calculateResourceEfficiencyScore(a);
            double scoreB = calculateResourceEfficiencyScore(b);
            return Double.compare(scoreB, scoreA);
        });
        
        // Priority queue for processors based on available resources
        List<ResourceAwareProcessor> sortedProcessors = new ArrayList<>(processors);
        sortedProcessors.sort((a, b) -> {
            // Sort by available resources (higher is better)
            // Weighted sum of normalized available resources
            double scoreA = calculateProcessorResourceScore(a);
            double scoreB = calculateProcessorResourceScore(b);
            return Double.compare(scoreB, scoreA);
        });
        
        // Try to assign jobs to processors
        for (ResourceAwareJob job : sortedJobs) {
            boolean assigned = false;
            
            // Try to find a processor that can accommodate this job
            for (Iterator<ResourceAwareProcessor> it = sortedProcessors.iterator(); it.hasNext();) {
                ResourceAwareProcessor processor = it.next();
                
                if (processor.canAccommodate(job)) {
                    // Assign job to this processor
                    processor.assignJob(job, currentTime);
                    it.remove(); // Remove processor from available list
                    assigned = true;
                    break;
                }
            }
            
            if (!assigned && !sortedProcessors.isEmpty()) {
                // Assign to best processor with resource constraints
                ResourceAwareProcessor bestProcessor = sortedProcessors.get(0);
                bestProcessor.assignJob(job, currentTime);
                sortedProcessors.remove(0);
            }
        }
    }
    
    /**
     * Calculate a resource efficiency score for a job
     * Higher score means more efficient use of resources
     */
    private double calculateResourceEfficiencyScore(ResourceAwareJob job) {
        // Priority and critical path are most important
        double score = job.getPriority() * 10;
        
        if (job instanceof DependencyAwareJob) {
            DependencyAwareJob dJob = (DependencyAwareJob) job;
            if (dJob.isCriticalJob()) {
                score += 100;
            }
            score += dJob.getCriticalPathLength() * 5;
        }
        
        // Jobs that use resources efficiently get a bonus
        double executionValue = job.getExecutionTime() / 
                               (job.getMemoryRequirement() / 1024.0 + 
                                job.getNetworkRequirement() / 100.0 + 
                                job.getCpuRequirement() / 100.0);
        
        score += executionValue;
        
        // Penalize jobs with very high resource requirements
        if (job.getMemoryRequirement() > 8192) score -= 50; // > 8GB memory
        if (job.getNetworkRequirement() > 1000) score -= 30; // > 1Gbps network
        if (job.getCpuRequirement() > 400) score -= 40; // > 4 cores
        
        return score;
    }
    
    /**
     * Calculate a score for processor resource availability
     * Higher score means more resources available
     */
    private double calculateProcessorResourceScore(ResourceAwareProcessor processor) {
        // Normalize each resource to 0-1 range and combine with weights
        double memoryScore = processor.getAvailableMemory() / (double) processor.getTotalMemoryCapacity() * 0.3;
        double networkScore = processor.getAvailableNetwork() / (double) processor.getTotalNetworkCapacity() * 0.3;
        double cpuScore = processor.getAvailableCpu() / (double) processor.getTotalCpuCapacity() * 0.4;
        
        // Add speed factor
        double speedScore = processor.getSpeedFactor() * 0.5;
        
        return memoryScore + networkScore + cpuScore + speedScore;
    }
    
    /**
     * Generate a report on resource utilization
     */
    public String generateResourceReport(List<Processor> processors) {
        StringBuilder report = new StringBuilder();
        report.append("Resource Utilization Report\n");
        report.append("==========================\n\n");
        
        double totalMemoryUtilization = 0;
        double totalNetworkUtilization = 0;
        double totalCpuUtilization = 0;
        int resourceProcessorCount = 0;
        
        for (Processor processor : processors) {
            if (processor instanceof ResourceAwareProcessor) {
                ResourceAwareProcessor resProcessor = (ResourceAwareProcessor) processor;
                ResourceUtilization utilization = resProcessor.getAverageResourceUtilization();
                
                report.append(String.format("Processor #%d:\n", processor.getId()));
                report.append(String.format("  Memory: %.2f%% utilized\n", utilization.getMemoryUtilization() * 100));
                report.append(String.format("  Network: %.2f%% utilized\n", utilization.getNetworkUtilization() * 100));
                report.append(String.format("  CPU: %.2f%% utilized\n", utilization.getCpuUtilization() * 100));
                report.append(String.format("  Overall: %.2f%% utilized\n\n", utilization.getOverallUtilization() * 100));
                
                totalMemoryUtilization += utilization.getMemoryUtilization();
                totalNetworkUtilization += utilization.getNetworkUtilization();
                totalCpuUtilization += utilization.getCpuUtilization();
                resourceProcessorCount++;
            }
        }
        
        if (resourceProcessorCount > 0) {
            report.append("Cluster-wide Resource Utilization:\n");
            report.append(String.format("  Memory: %.2f%%\n", totalMemoryUtilization / resourceProcessorCount * 100));
            report.append(String.format("  Network: %.2f%%\n", totalNetworkUtilization / resourceProcessorCount * 100));
            report.append(String.format("  CPU: %.2f%%\n", totalCpuUtilization / resourceProcessorCount * 100));
            report.append(String.format("  Overall: %.2f%%\n", 
                                      (totalMemoryUtilization + totalNetworkUtilization + totalCpuUtilization) / 
                                      (resourceProcessorCount * 3) * 100));
        }
        
        return report.toString();
    }
}