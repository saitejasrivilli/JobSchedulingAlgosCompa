import java.util.*;
import java.util.stream.Collectors;

/**
 * Integrated scheduler that combines neural network prediction with
 * resource-aware scheduling and dependency management.
 * 
 * This represents the most advanced scheduler in the system, incorporating
 * all enhanced features.
 */
public class IntegratedAdvancedScheduler implements Scheduler {
    private String name;
    private Scheduler baseScheduler;
    private NeuralNetworkPredictor neuralNetPredictor;
    
    /**
     * Create an integrated advanced scheduler
     * 
     * @param baseScheduler The base scheduling algorithm to use
     */
    public IntegratedAdvancedScheduler(Scheduler baseScheduler) {
        this.baseScheduler = baseScheduler;
        this.name = "Integrated-Advanced " + baseScheduler.getName();
        this.neuralNetPredictor = new NeuralNetworkPredictor();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Load historical job data for neural network training
     * 
     * @param filePath Path to historical data file
     */
    public void loadHistoricalData(String filePath) {
        this.neuralNetPredictor.loadHistoricalData(filePath);
    }
    
    
    @Override
    public void scheduleJobs(List<Job> jobs, List<Processor> processors, int currentTime, List<Job> completedJobs) {
        // Update ML model with completed jobs
        for (Job job : completedJobs) {
            if (job.getCompletionTime() != -1 && job.getCompletionTime() <= currentTime) {
                if (job instanceof ResourceAwareJob) {
                    neuralNetPredictor.recordJobCompletion((ResourceAwareJob) job);
                }
            }
        }
        
        // Get currently running jobs
        List<Job> runningJobs = processors.stream()
                                        .map(Processor::getCurrentJob)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());
        
        // Filter available jobs considering dependencies and conflicts
        List<Job> availableJobs = jobs.stream()
                                    .filter(job -> job.getArrivalTime() <= currentTime && 
                                                  !job.isCompleted() && 
                                                  job.areDependenciesMet(completedJobs) &&
                                                  job.getStartTime() == -1)
                                    .filter(job -> !(job instanceof DependencyAwareJob) || 
                                                 !((DependencyAwareJob)job).hasConflicts(runningJobs))
                                    .collect(Collectors.toList());
        
        // Update job execution time estimates using neural network predictions
        for (Job job : availableJobs) {
            if (job instanceof ResourceAwareJob) {
                int predictedTime = neuralNetPredictor.predictExecutionTime((ResourceAwareJob) job);
                job.setEstimatedExecutionTime(predictedTime);
            }
        }
        
        // Get available processors
        List<Processor> availableProcessors = processors.stream()
                                                     .filter(p -> !p.isBusy(currentTime))
                                                     .collect(Collectors.toList());
        
        // Perform advanced integrated scheduling
        integratedScheduling(availableJobs, availableProcessors, currentTime, completedJobs);
    }
    
    /**
     * Perform integrated scheduling considering all advanced features
     */
    private void integratedScheduling(List<Job> jobs, List<Processor> processors, 
                                    int currentTime, List<Job> completedJobs) {
        if (jobs.isEmpty() || processors.isEmpty()) {
            return;
        }
        
        // Score each job-processor pair
        Map<JobProcessorPair, Double> pairScores = new HashMap<>();
        
        for (Job job : jobs) {
            for (Processor processor : processors) {
                JobProcessorPair pair = new JobProcessorPair(job, processor);
                double score = calculateIntegratedScore(job, processor, currentTime, completedJobs);
                pairScores.put(pair, score);
            }
        }
        
        // Sort pairs by score (descending)
        List<Map.Entry<JobProcessorPair, Double>> sortedPairs = 
            pairScores.entrySet().stream()
                    .sorted(Map.Entry.<JobProcessorPair, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());
        
        // Track assigned jobs and processors
        Set<Job> assignedJobs = new HashSet<>();
        Set<Processor> assignedProcessors = new HashSet<>();
        
        // Assign jobs to processors in order of score
        for (Map.Entry<JobProcessorPair, Double> entry : sortedPairs) {
            JobProcessorPair pair = entry.getKey();
            Job job = pair.getJob();
            Processor processor = pair.getProcessor();
            
            // Skip if job or processor already assigned
            if (assignedJobs.contains(job) || assignedProcessors.contains(processor)) {
                continue;
            }
            
            // Check resource constraints
            boolean canAssign = true;
            if (job instanceof ResourceAwareJob && processor instanceof ResourceAwareProcessor) {
                ResourceAwareJob resourceJob = (ResourceAwareJob) job;
                ResourceAwareProcessor resourceProcessor = (ResourceAwareProcessor) processor;
                
                canAssign = resourceProcessor.canAccommodate(resourceJob);
            }
            
            // Assign job if possible
            if (canAssign) {
                processor.assignJob(job, currentTime);
                assignedJobs.add(job);
                assignedProcessors.add(processor);
                
                // Update earliest start times for dependent jobs
                if (job instanceof DependencyAwareJob) {
                    DependencyAwareJob dJob = (DependencyAwareJob) job;
                    for (DependencyAwareJob dependent : dJob.getDependents()) {
                        dependent.calculateEarliestStartTime();
                    }
                }
            }
        }
    }
    
    /**
     * Calculate an integrated score for a job-processor pair
     * Higher score means better assignment
     */
    private double calculateIntegratedScore(Job job, Processor processor, 
                                          int currentTime, List<Job> completedJobs) {
        double score = 0;
        
        // Base score is negative estimated completion time (earlier is better)
        int estimatedCompletionTime = currentTime + processor.estimateProcessingTime(job);
        score -= estimatedCompletionTime;
        
        // Add priority factor
        score += job.getPriority() * 10;
        
        // Resource efficiency factor
        if (job instanceof ResourceAwareJob && processor instanceof ResourceAwareProcessor) {
            ResourceAwareJob resourceJob = (ResourceAwareJob) job;
            ResourceAwareProcessor resourceProcessor = (ResourceAwareProcessor) processor;
            
            // Calculate resource matching score
            double resourceFit = calculateResourceFit(resourceJob, resourceProcessor);
            score += resourceFit * 100;
            
            // Penalize if processor cannot accommodate job's resources
            if (!resourceProcessor.canAccommodate(resourceJob)) {
                score -= 500; // Large penalty, but not infinite to allow assignment if necessary
            }
        }
        
        // Dependency factors
        if (job instanceof DependencyAwareJob) {
            DependencyAwareJob dJob = (DependencyAwareJob) job;
            
            // Critical path bonus
            if (dJob.isCriticalJob()) {
                score += 200;
            }
            
            // Critical path length factor
            score += dJob.getCriticalPathLength() * 3;
            
            // Dependent count bonus (more dependents = more important)
            score += dJob.getDependents().size() * 15;
            
            // Preferred dependencies bonus
            for (Map.Entry<Job, DependencyAwareJob.DependencyType> entry : dJob.getDependencyMap().entrySet()) {
                if (entry.getValue() == DependencyAwareJob.DependencyType.PREFERS && 
                    completedJobs.contains(entry.getKey())) {
                    score += 25;
                }
            }
        }
        
        // Processor speed factor (faster processors get priority)
        score += processor.getSpeedFactor() * 50;
        
        return score;
    }
    
    /**
     * Calculate how well a job's resource requirements match a processor's capabilities
     * Returns value between 0 and 1, where 1 is perfect fit
     */
    private double calculateResourceFit(ResourceAwareJob job, ResourceAwareProcessor processor) {
        // Calculate resource utilization percentages
        double memoryUtil = job.getMemoryRequirement() / (double) processor.getTotalMemoryCapacity();
        double networkUtil = job.getNetworkRequirement() / (double) processor.getTotalNetworkCapacity();
        double cpuUtil = job.getCpuRequirement() / (double) processor.getTotalCpuCapacity();
        
        // Clip to 0-1 range
        memoryUtil = Math.min(1.0, Math.max(0.0, memoryUtil));
        networkUtil = Math.min(1.0, Math.max(0.0, networkUtil));
        cpuUtil = Math.min(1.0, Math.max(0.0, cpuUtil));
        
        // Calculate average utilization
        double avgUtil = (memoryUtil + networkUtil + cpuUtil) / 3.0;
        
        // Best fit is when utilization is moderate (not too low, not too high)
        // Use a bell curve with maximum at 0.7 (70% utilization is ideal)
        double targetUtil = 0.7;
        double bellWidth = 0.3;
        double fit = Math.exp(-Math.pow(avgUtil - targetUtil, 2) / (2 * bellWidth * bellWidth));
        
        return fit;
    }
    
    /**
     * Generate a comprehensive performance report
     */
    public String generateComprehensiveReport(List<ResourceAwareJob> jobs, List<ResourceAwareProcessor> processors) {
        StringBuilder report = new StringBuilder();
        report.append("Comprehensive Performance Report\n");
        report.append("===============================\n\n");
        
        // Neural network prediction accuracy
        report.append(neuralNetPredictor.generateAccuracyReport()).append("\n\n");
        
        // Resource utilization
        ResourceAwareScheduler resourceScheduler = new ResourceAwareScheduler(baseScheduler);
        report.append(resourceScheduler.generateResourceReport(new ArrayList<>(processors))).append("\n\n");
        
        // Dependency metrics
        report.append("Dependency Analysis:\n");
        
        // Count dependency types
        int requires = 0;
        int prefers = 0;
        int conflicts = 0;
        
        for (ResourceAwareJob job : jobs) {
            for (Map.Entry<Job, DependencyAwareJob.DependencyType> entry : job.getDependencyMap().entrySet()) {
                switch (entry.getValue()) {
                    case REQUIRES: requires++; break;
                    case PREFERS: prefers++; break;
                    case CONFLICTS_WITH: conflicts++; break;
                }
            }
        }
        
        report.append(String.format("Total dependencies: %d (REQUIRES: %d, PREFERS: %d, CONFLICTS_WITH: %d)\n",
                              requires + prefers + conflicts, requires, prefers, conflicts));
        
        // Analyze critical path
        List<DependencyAwareJob> criticalJobs = jobs.stream()
                                           .filter(DependencyAwareJob::isCriticalJob)
                                           .collect(Collectors.toList());
        
        report.append(String.format("Critical jobs: %d (%.1f%% of total)\n", 
                              criticalJobs.size(), 
                              100.0 * criticalJobs.size() / jobs.size()));
        
        // Maximum critical path length
        int maxCriticalPath = jobs.stream()
                            .mapToInt(DependencyAwareJob::getCriticalPathLength)
                            .max()
                            .orElse(0);
        
        report.append(String.format("Maximum critical path length: %d time units\n\n", maxCriticalPath));
        
        // Performance metrics
        int makespan = jobs.stream().mapToInt(Job::getCompletionTime).max().orElse(0);
        double averageWaitingTime = jobs.stream().mapToInt(Job::getWaitingTime).average().orElse(0);
        double averageTurnaroundTime = jobs.stream().mapToInt(Job::getTurnaroundTime).average().orElse(0);
        double throughput = (double) jobs.size() / makespan;
        
        int totalBusyTime = processors.stream().mapToInt(Processor::getTotalBusyTime).sum();
        double resourceUtilization = (double) totalBusyTime / (processors.size() * makespan);
        
        report.append("Performance Metrics:\n");
        report.append(String.format("Makespan: %d time units\n", makespan));
        report.append(String.format("Average waiting time: %.2f time units\n", averageWaitingTime));
        report.append(String.format("Average turnaround time: %.2f time units\n", averageTurnaroundTime));
        report.append(String.format("Throughput: %.2f jobs/time unit\n", throughput));
        report.append(String.format("Resource utilization: %.2f%%\n", resourceUtilization * 100));
        
        return report.toString();
    }
}