import java.util.List;

/**
 * Interface for scheduling algorithms
 */
public interface Scheduler {
    String getName();
    void scheduleJobs(List<Job> jobs, List<Processor> processors, int currentTime, List<Job> completedJobs);
}