/**
 * Class representing a processor or machine that executes jobs
 */
public class Processor {
    private int id;
    private double speedFactor;
    private Job currentJob;
    private int busyUntil;
    private int totalBusyTime;
    
    public Processor(int id, double speedFactor) {
        this.id = id;
        this.speedFactor = speedFactor;
        this.currentJob = null;
        this.busyUntil = 0;
        this.totalBusyTime = 0;
    }
    
    // Copy constructor
    public Processor(Processor other) {
        this.id = other.id;
        this.speedFactor = other.speedFactor;
        this.currentJob = null;
        this.busyUntil = 0;
        this.totalBusyTime = 0;
    }
    
    public int getId() {
        return id;
    }
    
    public double getSpeedFactor() {
        return speedFactor;
    }
    
    public boolean isBusy(int currentTime) {
        return currentTime < busyUntil;
    }
    
    public int getBusyUntil() {
        return busyUntil;
    }
    
    public Job getCurrentJob() {
        return currentJob;
    }
    
    public void assignJob(Job job, int currentTime) {
        this.currentJob = job;
        int processingTime = calculateProcessingTime(job);
        this.busyUntil = currentTime + processingTime;
        this.totalBusyTime += processingTime;
        
        // Set job start time if not set
        if (job.getStartTime() == -1) {
            job.setStartTime(currentTime);
        }
    }
    
    public void completeJob(int currentTime) {
        if (currentJob != null) {
            currentJob.setCompletionTime(currentTime);
            currentJob = null;
        }
    }
    
    public int calculateProcessingTime(Job job) {
        return (int) Math.ceil(job.getExecutionTime() / speedFactor);
    }
    
    public int estimateProcessingTime(Job job) {
        return (int) Math.ceil(job.getEstimatedExecutionTime() / speedFactor);
    }
    
    public int getTotalBusyTime() {
        return totalBusyTime;
    }
    
    @Override
    public String toString() {
        return "Processor #" + id + " (Speed: " + speedFactor + ")";
    }
}