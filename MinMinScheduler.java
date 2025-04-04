import java.util.*;
import java.util.stream.Collectors;

/**
 * Min-Min Scheduler
 */
public class MinMinScheduler implements Scheduler {
    @Override
    public String getName() {
        return "Min-Min Algorithm";
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
        
        // Min-Min Algorithm:
        // 1. Find job with minimum completion time
        // 2. Assign it to the corresponding processor
        // 3. Remove the job from availableJobs
        // 4. Repeat until all jobs are assigned or no processors available
        
        while (!availableJobs.isEmpty() && !availableProcessors.isEmpty()) {
            Job minJob = null;
            Processor minProcessor = null;
            int minCompletionTime = Integer.MAX_VALUE;
            
            // Find job with minimum completion time
            for (Job job : availableJobs) {
                for (Processor processor : availableProcessors) {
                    int completionTime = completionTimes.get(job).get(processor);
                    if (completionTime < minCompletionTime) {
                        minCompletionTime = completionTime;
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
            } else {
                break;
            }
        }
    }
}