import java.util.*;
import java.util.stream.Collectors;

/**
 * Max-Min Scheduler
 */
public class MaxMinScheduler implements Scheduler {
    @Override
    public String getName() {
        return "Max-Min Algorithm";
    }
    
    @Override
    public void scheduleJobs(List<Job> jobs, List<Processor> processors, int currentTime, List<Job> completedJobs) {
        // Get available processors
        List<Processor> availableProcessors = processors.stream()
                                                      .filter(p -> !p.isBusy(currentTime))
                                                      .collect(Collectors.toList());
        
        if (availableProcessors.isEmpty()) {
            return;
        }
        
        // Get available jobs (arrived and not completed)
        List<Job> availableJobs = jobs.stream()
                                    .filter(job -> job.getArrivalTime() <= currentTime && 
                                                  !job.isCompleted() && 
                                                  job.areDependenciesMet(completedJobs) &&
                                                  job.getStartTime() == -1)
                                    .collect(Collectors.toList());
        
        if (availableJobs.isEmpty()) {
            return;
        }
        
        // Create completion time matrix for all jobs on all processors
        Map<Job, Map<Processor, Integer>> completionTimes = new HashMap<>();
        
        for (Job job : availableJobs) {
            Map<Processor, Integer> processorTimes = new HashMap<>();
            for (Processor processor : availableProcessors) {
                int procTime = processor.estimateProcessingTime(job);
                processorTimes.put(processor, currentTime + procTime);
            }
            completionTimes.put(job, processorTimes);
        }
        
        // Max-Min Algorithm:
        // 1. Find job with maximum completion time
        // 2. Assign it to the corresponding processor
        // 3. Remove the job from availableJobs
        // 4. Repeat until all jobs are assigned or no processors available
        
        while (!availableJobs.isEmpty() && !availableProcessors.isEmpty()) {
            Job maxJob = null;
            Processor minProcessor = null;
            int maxMinCompletionTime = Integer.MIN_VALUE;
            
            // For each job, find its minimum completion time across all processors
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
            } else {
                break;
            }
        }
    }
}