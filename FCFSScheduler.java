import java.util.*;
import java.util.stream.Collectors;

/**
 * First Come First Served Scheduler
 */
public class FCFSScheduler implements Scheduler {
    @Override
    public String getName() {
        return "First Come First Served";
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
                                    .sorted(Comparator.comparingInt(Job::getArrivalTime))
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