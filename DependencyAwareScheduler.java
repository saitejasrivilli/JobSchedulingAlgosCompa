import java.util.*;
import java.util.stream.Collectors;

/**
 * Dependency-aware job scheduler that handles job dependencies
 */
public class DependencyAwareScheduler implements Scheduler {
    private String name;
    private Scheduler baseScheduler;
    
    /**
     * Create a dependency-aware scheduler wrapping a base scheduler
     * 
     * @param baseScheduler The base scheduling algorithm to use
     */
    public DependencyAwareScheduler(Scheduler baseScheduler) {
        this.baseScheduler = baseScheduler;
        this.name = "Dependency-Aware " + baseScheduler.getName();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void scheduleJobs(List<Job> jobs, List<Processor> processors, int currentTime, List<Job> completedJobs) {
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
        
        // Enhance priority for dependency-aware jobs
        if (!availableJobs.isEmpty() && availableJobs.get(0) instanceof DependencyAwareJob) {
            // Sort by enhanced priority score if using priority-based algorithms
            if (baseScheduler instanceof PriorityScheduler || 
                baseScheduler instanceof HybridMinMinSJFScheduler) {
                
                availableJobs.sort((a, b) -> {
                    double scoreA = ((DependencyAwareJob)a).calculatePriorityScore(completedJobs);
                    double scoreB = ((DependencyAwareJob)b).calculatePriorityScore(completedJobs);
                    return Double.compare(scoreB, scoreA); // Higher score first
                });
            }
            
            // Prioritize critical path jobs
            List<Job> criticalJobs = availableJobs.stream()
                                               .filter(job -> job instanceof DependencyAwareJob && 
                                                            ((DependencyAwareJob)job).isCriticalJob())
                                               .collect(Collectors.toList());
            
            if (!criticalJobs.isEmpty()) {
                // Move critical jobs to front of list
                availableJobs.removeAll(criticalJobs);
                availableJobs.addAll(0, criticalJobs);
            }
        }
        
        // Get available processors
        List<Processor> availableProcessors = processors.stream()
                                                      .filter(p -> !p.isBusy(currentTime))
                                                      .collect(Collectors.toList());
        
        // Delegate actual scheduling to base scheduler with our filtered list
        if (baseScheduler instanceof MinMinScheduler || 
            baseScheduler instanceof MaxMinScheduler) {
            // For Min-Min and Max-Min, we need to modify the algorithm to consider dependencies
            assignJobsWithDependencyAwareness(availableJobs, availableProcessors, currentTime, completedJobs);
        } else {
            // For other algorithms, we can just filter the job list and use the base scheduler
            baseScheduler.scheduleJobs(availableJobs, availableProcessors, currentTime, completedJobs);
        }
    }
    
    /**
     * Assign jobs with dependency awareness for Min-Min and Max-Min algorithms
     */
    private void assignJobsWithDependencyAwareness(List<Job> availableJobs, List<Processor> availableProcessors, 
                                                 int currentTime, List<Job> completedJobs) {
        if (availableJobs.isEmpty() || availableProcessors.isEmpty()) {
            return;
        }
        
        // Create completion time matrix for all jobs on all processors
        Map<Job, Map<Processor, Integer>> completionTimes = new HashMap<>();
        
        for (Job job : availableJobs) {
            Map<Processor, Integer> processorTimes = new HashMap<>();
            for (Processor processor : availableProcessors) {
                int procTime = processor.estimateProcessingTime(job);
                
                // For dependency-aware jobs, consider earliest start time
                if (job instanceof DependencyAwareJob) {
                    DependencyAwareJob dJob = (DependencyAwareJob) job;
                    int earliestStart = Math.max(currentTime, dJob.getEarliestStartTime());
                    processorTimes.put(processor, earliestStart + procTime);
                } else {
                    processorTimes.put(processor, currentTime + procTime);
                }
            }
            completionTimes.put(job, processorTimes);
        }
        
        // Based on the base scheduler type
        if (baseScheduler instanceof MinMinScheduler) {
            // Min-Min Algorithm with dependencies
            while (!availableJobs.isEmpty() && !availableProcessors.isEmpty()) {
                Job minJob = null;
                Processor minProcessor = null;
                int minCompletionTime = Integer.MAX_VALUE;
                
                // Find job with minimum completion time
                for (Job job : availableJobs) {
                    for (Processor processor : availableProcessors) {
                        int completionTime = completionTimes.get(job).get(processor);
                        
                        // Prioritize critical jobs
                        double criticalFactor = job instanceof DependencyAwareJob && 
                                               ((DependencyAwareJob)job).isCriticalJob() ? 0.9 : 1.0;
                                               
                        int adjustedTime = (int)(completionTime * criticalFactor);
                        
                        if (adjustedTime < minCompletionTime) {
                            minCompletionTime = adjustedTime;
                            minJob = job;
                            minProcessor = processor;
                        }
                    }
                }
                
                if (minJob != null && minProcessor != null) {
                    // Assign job to processor
                    minProcessor.assignJob(minJob, currentTime);
                    availableJobs.remove(minJob);
                    availableProcessors.remove(minProcessor);
                    
                    // Update earliest start times for dependent jobs
                    if (minJob instanceof DependencyAwareJob) {
                        DependencyAwareJob dJob = (DependencyAwareJob) minJob;
                        for (DependencyAwareJob dependent : dJob.getDependents()) {
                            dependent.calculateEarliestStartTime();
                        }
                    }
                } else {
                    break;
                }
            }
        } else if (baseScheduler instanceof MaxMinScheduler) {
            // Max-Min Algorithm with dependencies
            while (!availableJobs.isEmpty() && !availableProcessors.isEmpty()) {
                Job maxJob = null;
                Processor minProcessor = null;
                int maxMinCompletionTime = Integer.MIN_VALUE;
                
                // For each job, find its minimum completion time
                for (Job job : availableJobs) {
                    int minCompletionTime = Integer.MAX_VALUE;
                    Processor bestProcessor = null;
                    
                    for (Processor processor : availableProcessors) {
                        int completionTime = completionTimes.get(job).get(processor);
                        if (completionTime < minCompletionTime) {
                            minCompletionTime = completionTime;
                            bestProcessor = processor;
                        }
                    }
                    
                    // Apply critical path factor
                    if (job instanceof DependencyAwareJob && ((DependencyAwareJob)job).isCriticalJob()) {
                        minCompletionTime = (int)(minCompletionTime * 1.1); // Increase priority
                    }
                    
                    // Keep track of the job with the maximum of these minimums
                    if (minCompletionTime > maxMinCompletionTime) {
                        maxMinCompletionTime = minCompletionTime;
                        maxJob = job;
                        minProcessor = bestProcessor;
                    }
                }
                
                if (maxJob != null && minProcessor != null) {
                    // Assign job to processor
                    minProcessor.assignJob(maxJob, currentTime);
                    availableJobs.remove(maxJob);
                    availableProcessors.remove(minProcessor);
                    
                    // Update earliest start times for dependent jobs
                    if (maxJob instanceof DependencyAwareJob) {
                        DependencyAwareJob dJob = (DependencyAwareJob) maxJob;
                        for (DependencyAwareJob dependent : dJob.getDependents()) {
                            dependent.calculateEarliestStartTime();
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }
} 