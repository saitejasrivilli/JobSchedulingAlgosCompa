import java.util.*;
import java.util.stream.Collectors;

/**
 * Round Robin Scheduler
 */
public class RoundRobinScheduler implements Scheduler {
    private int timeQuantum;
    private Queue<Job> readyQueue;
    
    public RoundRobinScheduler(int timeQuantum) {
        this.timeQuantum = timeQuantum;
        this.readyQueue = new LinkedList<>();
    }
    
    @Override
    public String getName() {
        return "Round Robin (Q=" + timeQuantum + ")";
    }
    
    @Override
    public void scheduleJobs(List<Job> jobs, List<Processor> processors, int currentTime, List<Job> completedJobs) {
        // Add newly arrived jobs to ready queue
        jobs.stream()
            .filter(job -> job.getArrivalTime() == currentTime && 
                          !job.isCompleted() && 
                          job.areDependenciesMet(completedJobs))
            .forEach(readyQueue::add);
        
        // Get available processors
        List<Processor> availableProcessors = processors.stream()
                                                      .filter(p -> !p.isBusy(currentTime))
                                                      .collect(Collectors.toList());
        
        // Process any completed jobs and add them back to ready queue if not finished
        for (Processor processor : processors) {
            if (processor.isBusy(currentTime)) {
                continue;
            }
            
            Job completedJob = processor.getCurrentJob();
            if (completedJob != null) {
                if (!completedJob.isCompleted()) {
                    // Job used its time quantum but isn't finished, put it back in the queue
                    readyQueue.add(completedJob);
                }
                processor.completeJob(currentTime);
            }
        }
        
        // Assign jobs to available processors
        for (Processor processor : availableProcessors) {
            if (readyQueue.isEmpty()) {
                break;
            }
            
            Job job = readyQueue.poll();
            // In Round Robin, we process for a time quantum
            int remainingTime = job.getRemainingTime();
            int processTime = Math.min(remainingTime, timeQuantum);
            
            // Create a temporary job with only the time quantum
            Job tempJob = new Job(job.getId(), job.getArrivalTime(), processTime, job.getPriority());
            if (job.getStartTime() != -1) {
                tempJob.setStartTime(job.getStartTime());
            }
            
            processor.assignJob(tempJob, currentTime);
            
            // Update the original job's remaining time
            for (int i = 0; i < processTime; i++) {
                job.decrementRemainingTime();
            }
            
            // If the job is now completed, add it to completed jobs
            if (job.isCompleted()) {
                job.setCompletionTime(currentTime + processTime);
            }
        }
    }
}