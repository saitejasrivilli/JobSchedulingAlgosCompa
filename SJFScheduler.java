import java.util.*;
import java.util.stream.Collectors;

/**
 * Shortest Job First Scheduler
 */
public class SJFScheduler implements Scheduler {
    @Override
    public String getName() {
        return "Shortest Job First";
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
                                    .sorted(Comparator.comparingInt(Job::getEstimatedExecutionTime))
                                    .collect(Collectors.toList());
        
        // Assign jobs to available processors
        for (Processor processor : availableProcessors) {
            if (availableJobs.isEmpty()) {
                break;
            }
            
            Job job = availableJobs.remove(0);
            processor.assignJob(job, currentTime);
        }
    }
}