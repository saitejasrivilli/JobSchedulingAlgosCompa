import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a job to be scheduled
 */
public class Job implements Comparable<Job> {
    private int id;
    private int arrivalTime;
    private int executionTime;
    private int estimatedExecutionTime;
    private int priority;
    private boolean isIOBound;
    private int remainingTime;
    private int waitingTime;
    private int completionTime;
    private int startTime;
    private List<Job> dependencies;
    
    public Job(int id, int arrivalTime, int executionTime, int priority) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.executionTime = executionTime;
        this.estimatedExecutionTime = executionTime; // Default is accurate estimate
        this.priority = priority;
        this.isIOBound = false;
        this.remainingTime = executionTime;
        this.waitingTime = 0;
        this.completionTime = -1;
        this.startTime = -1;
        this.dependencies = new ArrayList<>();
    }
    
    // Copy constructor
    public Job(Job other) {
        this.id = other.id;
        this.arrivalTime = other.arrivalTime;
        this.executionTime = other.executionTime;
        this.estimatedExecutionTime = other.estimatedExecutionTime;
        this.priority = other.priority;
        this.isIOBound = other.isIOBound;
        this.remainingTime = other.executionTime;
        this.waitingTime = 0;
        this.completionTime = -1;
        this.startTime = -1;
        this.dependencies = new ArrayList<>(other.dependencies);
    }
    
    @Override
    public int compareTo(Job other) {
        return Integer.compare(this.id, other.id);
    }
    /**
 * Checks if all dependencies for this job have been met
 * 
 * @param completedJobs List of completed jobs
 * @return true if all required dependencies are met, false otherwise
 */
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public int getArrivalTime() {
        return arrivalTime;
    }
    
    public int getExecutionTime() {
        return executionTime;
    }
    
    public int getEstimatedExecutionTime() {
        return estimatedExecutionTime;
    }
    
    public void setEstimatedExecutionTime(int estimatedExecutionTime) {
        this.estimatedExecutionTime = Math.max(1, estimatedExecutionTime); // Ensure positive
    }
    
    public int getPriority() {
        return priority;
    }
    
    public boolean isIOBound() {
        return isIOBound;
    }
    
    public void setIOBound(boolean isIOBound) {
        this.isIOBound = isIOBound;
    }
    
    public int getRemainingTime() {
        return remainingTime;
    }
    
    public void decrementRemainingTime() {
        if (remainingTime > 0) {
            remainingTime--;
        }
    }
    
    public boolean isCompleted() {
        return remainingTime == 0;
    }
    
    public void setCompletionTime(int completionTime) {
        this.completionTime = completionTime;
    }
    
    public int getCompletionTime() {
        return completionTime;
    }
    
    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }
    
    public int getStartTime() {
        return startTime;
    }
    
    public int getWaitingTime() {
        return waitingTime;
    }
    
    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }
    
    public void incrementWaitingTime() {
        waitingTime++;
    }
    
    public int getTurnaroundTime() {
        if (completionTime == -1) {
            return -1;
        }
        return completionTime - arrivalTime;
    }
    
    public List<Job> getDependencies() {
        return dependencies;
    }
    
    public void addDependency(Job job) {
        dependencies.add(job);
    }
    
    /**
 * Checks if all dependencies for this job have been met
 * 
 * @param completedJobs List of completed jobs
 * @return true if all required dependencies are met, false otherwise
 */
public boolean areDependenciesMet(List<Job> completedJobs) {
    // For a standard Job without dependencies, always return true
    // This method will be overridden in DependencyAwareJob
    return true;
}
    @Override
    public String toString() {
        return "Job #" + id + " (Arrival: " + arrivalTime + ", Exec: " + executionTime + ", Priority: " + priority + ")";
    }
}