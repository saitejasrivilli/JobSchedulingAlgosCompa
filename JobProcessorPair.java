import java.util.Objects;

/**
 * Helper class to represent a job-processor pair
 */
class JobProcessorPair {
    private Job job;
    private Processor processor;
    
    public JobProcessorPair(Job job, Processor processor) {
        this.job = job;
        this.processor = processor;
    }
    
    public Job getJob() {
        return job;
    }
    
    public Processor getProcessor() {
        return processor;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobProcessorPair that = (JobProcessorPair) o;
        return job.equals(that.job) && processor.equals(that.processor);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(job, processor);
    }
}