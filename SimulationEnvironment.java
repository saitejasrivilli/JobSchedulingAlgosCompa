import java.util.*;
import java.util.stream.Collectors;

/**
 * SimulationEnvironment class to manage job scheduling simulations
 */
public class SimulationEnvironment {
    private static final int MAX_SIMULATION_TIME = 1000; // Maximum simulation time
    
    /**
     * Generate a list of random jobs for simulation
     * 
     * @param count Number of jobs to generate
     * @param minExecTime Minimum execution time
     * @param maxExecTime Maximum execution time
     * @return List of generated jobs
     */
    public List<Job> generateJobs(int count, int minExecTime, int maxExecTime) {
        List<Job> jobs = new ArrayList<>();
        
        Random random = new Random(42); // Use a fixed seed for reproducibility
        
        for (int i = 0; i < count; i++) {
            int arrivalTime = random.nextInt(count / 2); // Distribute arrivals
            int executionTime = minExecTime + random.nextInt(maxExecTime - minExecTime + 1);
            int priority = random.nextInt(10) + 1; // Priority from 1 to 10
            
            Job job = new Job(i, arrivalTime, executionTime, priority);
            jobs.add(job);
        }
        
        return jobs;
    }
    
    /**
     * Create processors with different speed factors
     * 
     * @param count Number of processors to create
     * @param baseSpeedFactor Base speed factor (1.0 means standard speed)
     * @return List of processors
     */
    public List<Processor> createProcessors(int count, double baseSpeedFactor) {
        List<Processor> processors = new ArrayList<>();
        Random random = new Random(42); // Use a fixed seed for reproducibility
        
        for (int i = 0; i < count; i++) {
            // Add some variation to processor speeds (±20%)
            double speedVariation = 0.8 + (random.nextDouble() * 0.4);
            double speedFactor = baseSpeedFactor * speedVariation;
            
            Processor processor = new Processor(i, speedFactor);
            processors.add(processor);
        }
        
        return processors;
    }
    
    /**
     * Run a simulation with a specific scheduler, jobs, and processors
     * 
     * @param scheduler The scheduling algorithm to use
     * @param originalJobs List of jobs to schedule
     * @param originalProcessors List of processors to use
     * @return Metrics from the simulation
     */
    public SchedulingMetrics runSimulation(Scheduler scheduler, List<Job> originalJobs, List<Processor> originalProcessors) {
        // Create deep copies to avoid modifying the originals
        List<Job> jobs = originalJobs.stream().map(job -> new Job(job)).collect(Collectors.toList());
        List<Processor> processors = originalProcessors.stream().map(proc -> new Processor(proc)).collect(Collectors.toList());
        
        int currentTime = 0;
        List<Job> completedJobs = new ArrayList<>();
        boolean allJobsCompleted = false;
        
        while (currentTime < MAX_SIMULATION_TIME && !allJobsCompleted) {
            // Process completed jobs
            for (Processor processor : processors) {
                if (processor.isBusy(currentTime)) {
                    continue;
                }
                
                Job completedJob = processor.getCurrentJob();
                if (completedJob != null) {
                    // Find the original job and mark it as completed
                    for (Job job : jobs) {
                        if (job.getId() == completedJob.getId() && !job.isCompleted()) {
                            // Set completion time if not already set
                            if (job.getCompletionTime() == -1) {
                                job.setCompletionTime(currentTime);
                            }
                            // Ensure remaining time is zero
                            while (job.getRemainingTime() > 0) {
                                job.decrementRemainingTime();
                            }
                            completedJobs.add(job);
                            break;
                        }
                    }
                    processor.completeJob(currentTime);
                }
            }
            
            // Schedule jobs
            scheduler.scheduleJobs(jobs, processors, currentTime, completedJobs);
            
            // Update waiting time for jobs that have arrived but not started
            for (Job job : jobs) {
                if (job.getArrivalTime() <= currentTime && !job.isCompleted() && job.getStartTime() == -1) {
                    job.incrementWaitingTime();
                }
            }
            
            // Check if all jobs are completed
            allJobsCompleted = jobs.stream().allMatch(Job::isCompleted);
            
            // Increment time
            currentTime++;
        }
        
        // Calculate metrics
        int makespan = jobs.stream().mapToInt(Job::getCompletionTime).max().orElse(0);
        double averageWaitingTime = jobs.stream().mapToInt(Job::getWaitingTime).average().orElse(0);
        double averageTurnaroundTime = jobs.stream().mapToInt(Job::getTurnaroundTime).average().orElse(0);
        double throughput = (double) jobs.size() / makespan;
        
        // Calculate resource utilization (total busy time / (total processors * makespan))
        int totalBusyTime = processors.stream().mapToInt(Processor::getTotalBusyTime).sum();
        double resourceUtilization = (double) totalBusyTime / (processors.size() * makespan);
        
        return new SchedulingMetrics(makespan, averageWaitingTime, averageTurnaroundTime, throughput, resourceUtilization);
    }
    
    /**
     * Visualize schedule as a Gantt chart (console version)
     * 
     * @param jobs List of completed jobs
     * @param processors List of processors
     * @param algorithm Name of the algorithm used
     */
    public void visualizeSchedule(List<Job> jobs, List<Processor> processors, String algorithm) {
        System.out.println("\n----- Schedule Visualization for " + algorithm + " -----");
        
        // Find makespan
        int makespan = jobs.stream().mapToInt(Job::getCompletionTime).max().orElse(0);
        
        // Print header
        System.out.print("Time: ");
        for (int t = 0; t < makespan; t += 5) {
            System.out.printf("%5d", t);
        }
        System.out.println();
        
        // Print schedule for each processor
        for (Processor processor : processors) {
            System.out.print("P" + processor.getId() + ":   ");
            
            // For each processor, show which job was running at each time
            for (int t = 0; t < makespan; t += 5) {
                boolean jobFound = false;
                
                for (Job job : jobs) {
                    if (job.getStartTime() <= t && job.getCompletionTime() > t) {
                        System.out.printf("%5d", job.getId());
                        jobFound = true;
                        break;
                    }
                }
                
                if (!jobFound) {
                    System.out.printf("%5s", "-");
                }
            }
            System.out.println();
        }
    }
}