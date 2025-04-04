import java.util.*;
import java.io.*;

/**
 * Advanced demonstration of job scheduling with neural network prediction and resource constraints
 * 
 * This application showcases the most advanced features of the scheduling system:
 *  - Neural network for runtime prediction
 *  - Resource constraints (memory, network, CPU)
 *  - Dependency management
 *  - Integrated scheduling
 * 
 * @author Sai Teja Srivillibhutturu
 */
public class AdvancedSchedulingDemo {
    public static void main(String[] args) {
        System.out.println("Advanced Job Scheduling with Neural Network and Resource Constraints");
        System.out.println("==================================================================");
        
        // Create simulation environment
        SimulationEnvironment environment = new SimulationEnvironment();
        
        // Generate historical data for neural network training
        System.out.println("\nGenerating historical data for neural network training...");
        generateHistoricalData(1000);
        
        // Create dependency graph generator
        DependencyGraphGenerator graphGenerator = new DependencyGraphGenerator(42);
        
        // Create job sets with different dependency patterns
        System.out.println("\nGenerating resource-aware job sets with different dependency patterns...");
        Map<String, List<ResourceAwareJob>> jobSets = new HashMap<>();
        
        String[] patterns = {"Linear", "Tree", "Diamond", "Pipeline", "Complex"};
        for (String pattern : patterns) {
            // Generate dependency-aware jobs
            List<DependencyAwareJob> depJobs = graphGenerator.generatePatternedDependencies(pattern.toLowerCase(), 30);
            
            // Convert to resource-aware jobs
            List<ResourceAwareJob> resourceJobs = new ArrayList<>();
            Random random = new Random(pattern.hashCode());
            
            for (DependencyAwareJob depJob : depJobs) {
                // Create resource-aware job with same properties
                ResourceAwareJob resourceJob = new ResourceAwareJob(
                    depJob.getId(),
                    depJob.getArrivalTime(),
                    depJob.getExecutionTime(),
                    depJob.getPriority()
                );
                
                // Copy dependencies
                for (Map.Entry<Job, DependencyAwareJob.DependencyType> entry : depJob.getDependencyMap().entrySet()) {
                    Job dependency = entry.getKey();
                    DependencyAwareJob.DependencyType type = entry.getValue();
                    
                    // Find corresponding resource-aware job
                    for (ResourceAwareJob resJob : resourceJobs) {
                        if (resJob.getId() == dependency.getId()) {
                            resourceJob.addDependency(resJob, type);
                            break;
                        }
                    }
                }
                
                // Set random resource requirements
                int memory = 512 + random.nextInt(7680); // 0.5GB to 8GB
                int network = 10 + random.nextInt(990); // 10Mbps to 1Gbps
                int cpu = 10 + random.nextInt(390); // 10% to 400% (4 cores)
                resourceJob.setResourceRequirements(memory, network, cpu);
                
                resourceJobs.add(resourceJob);
            }
            
            // Calculate critical paths
            for (ResourceAwareJob job : resourceJobs) {
                job.calculateCriticalPath();
            }
            
            jobSets.put(pattern, resourceJobs);
        }
        
        // Create resource-aware processors with different capabilities
        List<ResourceAwareProcessor> processors = createResourceAwareProcessors(6);
        
        // Create base schedulers
        List<Scheduler> baseSchedulers = Arrays.asList(
            new FCFSScheduler(),
            new SJFScheduler(),
            new MinMinScheduler(),
            new HybridMinMinSJFScheduler()
        );
        
        // Create advanced schedulers
        List<Scheduler> advancedSchedulers = new ArrayList<>();
        
        // Neural network-based scheduler
        for (Scheduler baseScheduler : baseSchedulers) {
            // Resource-aware scheduler
            ResourceAwareScheduler resourceScheduler = new ResourceAwareScheduler(baseScheduler);
            advancedSchedulers.add(resourceScheduler);
            
            // Fully integrated scheduler
            IntegratedAdvancedScheduler integratedScheduler = new IntegratedAdvancedScheduler(baseScheduler);
            integratedScheduler.loadHistoricalData("job_history_advanced.csv");
            advancedSchedulers.add(integratedScheduler);
        }
        
        // Run comparison for each job set
        for (Map.Entry<String, List<ResourceAwareJob>> entry : jobSets.entrySet()) {
            String patternName = entry.getKey();
            List<ResourceAwareJob> jobs = entry.getValue();
            
            System.out.println("\n\nTesting " + patternName + " Pattern with Resource Constraints");
            System.out.println("=================================================");
            
            System.out.println("\nResource-Aware Processors:");
            for (ResourceAwareProcessor processor : processors) {
                System.out.println(processor);
            }
            
            System.out.println("\nResource-Aware Jobs (Sample):");
            for (int i = 0; i < Math.min(5, jobs.size()); i++) {
                System.out.println(jobs.get(i));
            }
            System.out.println("... and " + (jobs.size() - 5) + " more jobs");
            
            // Run with base schedulers
            System.out.println("\nBase Schedulers:");
            Map<String, SchedulingMetrics> baseResults = new HashMap<>();
            
            for (Scheduler scheduler : baseSchedulers) {
                // Create a copy of the jobs
                List<ResourceAwareJob> jobsCopy = new ArrayList<>();
                for (ResourceAwareJob job : jobs) {
                    jobsCopy.add(new ResourceAwareJob(job));
                }
                
                SchedulingMetrics metrics = environment.runSimulation(scheduler, new ArrayList<>(jobsCopy), new ArrayList<>(processors));
                baseResults.put(scheduler.getName(), metrics);
            }
            printResults(baseResults);
            
            // Run with advanced schedulers
            System.out.println("\nAdvanced Schedulers:");
            Map<String, SchedulingMetrics> advancedResults = new HashMap<>();
            
            for (Scheduler scheduler : advancedSchedulers) {
                // Create a copy of the jobs
                List<ResourceAwareJob> jobsCopy = new ArrayList<>();
                for (ResourceAwareJob job : jobs) {
                    jobsCopy.add(new ResourceAwareJob(job));
                }
                
                SchedulingMetrics metrics = environment.runSimulation(scheduler, new ArrayList<>(jobsCopy), new ArrayList<>(processors));
                advancedResults.put(scheduler.getName(), metrics);
            }
            printResults(advancedResults);
            
            // Calculate improvement
            System.out.println("\nPerformance Improvements:");
            for (Scheduler baseScheduler : baseSchedulers) {
                String baseName = baseScheduler.getName();
                
                if (baseResults.containsKey(baseName)) {
                    // Resource-aware improvement
                    String resourceName = "Resource-Aware " + baseName;
                    if (advancedResults.containsKey(resourceName)) {
                        calculateImprovement(baseName, resourceName, baseResults, advancedResults);
                    }
                    
                    // Integrated improvement
                    String integratedName = "Integrated-Advanced " + baseName;
                    if (advancedResults.containsKey(integratedName)) {
                        calculateImprovement(baseName, integratedName, baseResults, advancedResults);
                    }
                }
            }
        }
        
        // Run a detailed analysis with the best scheduler
        System.out.println("\n\nDetailed Analysis with Integrated-Advanced Hybrid Scheduler");
        System.out.println("=========================================================");
        
        // Get complex jobs
        List<ResourceAwareJob> complexJobs = jobSets.get("Complex");
        
        // Create a fresh copy of the jobs
        List<ResourceAwareJob> jobsCopy = new ArrayList<>();
        for (ResourceAwareJob job : complexJobs) {
            jobsCopy.add(new ResourceAwareJob(job));
        }
        
        // Create integrated scheduler
        IntegratedAdvancedScheduler integratedScheduler = new IntegratedAdvancedScheduler(new HybridMinMinSJFScheduler());
        integratedScheduler.loadHistoricalData("job_history_advanced.csv");
        
        // Run simulation
        List<ResourceAwareProcessor> processorsCopy = new ArrayList<>();
        for (ResourceAwareProcessor processor : processors) {
            processorsCopy.add(new ResourceAwareProcessor(processor));
        }
        
        environment.runSimulation(integratedScheduler, new ArrayList<>(jobsCopy), new ArrayList<>(processorsCopy));
        
        // Generate comprehensive report
        String report = integratedScheduler.generateComprehensiveReport(jobsCopy, processorsCopy);
        System.out.println(report);
        
        // Save report to file
        try (PrintWriter writer = new PrintWriter(new FileWriter("advanced_scheduling_report.txt"))) {
            writer.println(report);
            System.out.println("Detailed report saved to advanced_scheduling_report.txt");
        } catch (IOException e) {
            System.err.println("Error saving report: " + e.getMessage());
        }
        
        System.out.println("\nAdvanced Demo complete!");
    }
    
    /**
     * Generate historical job data for neural network training
     */
    private static void generateHistoricalData(int count) {
        // Create job history file
        try (PrintWriter writer = new PrintWriter(new FileWriter("job_history_advanced.csv"))) {
            // Write header
            writer.println("JobId,EstimatedTime,ActualTime,Priority,IsIOBound,NumDependencies,MemoryReq,NetworkReq");
            
            // Generate random jobs
            Random random = new Random(42);
            for (int i = 0; i < count; i++) {
                int estimatedTime = 1 + random.nextInt(20);
                
                // Add some randomness to actual execution time
                // The model should learn the patterns in these errors
                double errorFactor;
                if (random.nextDouble() < 0.1) {
                    // 10% chance of large error (3x-5x)
                    errorFactor = 3.0 + random.nextDouble() * 2.0;
                } else if (random.nextDouble() < 0.3) {
                    // 20% chance of moderate error (1.5x-3x)
                    errorFactor = 1.5 + random.nextDouble() * 1.5;
                } else {
                    // 70% chance of small error (0.8x-1.2x)
                    errorFactor = 0.8 + random.nextDouble() * 0.4;
                }
                
                int actualTime = (int) Math.max(1, Math.round(estimatedTime * errorFactor));
                
                // Other job properties
                int priority = random.nextInt(10) + 1;
                boolean isIOBound = random.nextDouble() < 0.3; // 30% of jobs are I/O bound
                int numDependencies = random.nextInt(4); // 0-3 dependencies
                
                // Resource requirements
                int memoryReq = 512 + random.nextInt(7680); // 0.5GB to 8GB
                int networkReq = 10 + random.nextInt(990); // 10Mbps to 1Gbps
                
                // Introduce correlation between resource requirements and execution time
                // High memory or network requirements tend to cause longer execution times
                if (memoryReq > 4096 || networkReq > 500) {
                    actualTime = (int)(actualTime * 1.5);
                }
                
                // Write job record
                writer.printf("%d,%d,%d,%d,%b,%d,%d,%d\n", 
                             i, estimatedTime, actualTime, priority, isIOBound, numDependencies,
                             memoryReq, networkReq);
            }
            
            System.out.println("Generated " + count + " historical job records with resource requirements");
        } catch (IOException e) {
            System.err.println("Error generating historical data: " + e.getMessage());
        }
    }
    
    /**
     * Create a set of resource-aware processors with different capabilities
     */
    private static List<ResourceAwareProcessor> createResourceAwareProcessors(int count) {
        List<ResourceAwareProcessor> processors = new ArrayList<>();
        
        // Define different processor types
        double[] speedFactors = {0.5, 1.0, 1.5, 2.0, 2.5, 3.0};
        int[][] memoryCapacities = {
            {4096, 8192, 16384, 32768, 65536, 131072} // Memory in MB (4GB to 128GB)
        };
        int[][] networkCapacities = {
            {100, 250, 500, 1000, 2000, 10000} // Network in Mbps (100Mbps to 10Gbps)
        };
        int[][] cpuCapacities = {
            {100, 200, 400, 600, 800, 1600} // CPU capacity (1 to 16 cores)
        };
        
        // Create processors
        for (int i = 0; i < count; i++) {
            int typeIndex = i % speedFactors.length;
            
            ResourceAwareProcessor processor = new ResourceAwareProcessor(
                i,
                speedFactors[typeIndex],
                memoryCapacities[0][typeIndex],
                networkCapacities[0][typeIndex],
                cpuCapacities[0][typeIndex]
            );
            
            processors.add(processor);
        }
        
        return processors;
    }
    
    /**
     * Print scheduling results in a formatted table
     */
    private static void printResults(Map<String, SchedulingMetrics> results) {
        System.out.printf("%-35s %-15s %-15s %-15s %-15s %-15s%n", 
                         "Algorithm", "Makespan", "Avg Wait Time", "Avg Turnaround", "Throughput", "Utilization %");
        System.out.println("-----------------------------------------------------------------------------------------------------");
        
        results.forEach((algorithm, metrics) -> {
            System.out.printf("%-35s %-15.2f %-15.2f %-15.2f %-15.2f %-15.2f%n", 
                            algorithm, 
                            metrics.getMakespan(),
                            metrics.getAverageWaitingTime(),
                            metrics.getAverageTurnaroundTime(),
                            metrics.getThroughput(),
                            metrics.getResourceUtilization() * 100);
        });
    }
    
    /**
     * Calculate and print improvement between two schedulers
     */
    private static void calculateImprovement(String baseAlgorithm, String advancedAlgorithm,
                                          Map<String, SchedulingMetrics> baseResults,
                                          Map<String, SchedulingMetrics> advancedResults) {
        SchedulingMetrics baseMetrics = baseResults.get(baseAlgorithm);
        SchedulingMetrics advancedMetrics = advancedResults.get(advancedAlgorithm);
        
        double makespanImprovement = 100 * (baseMetrics.getMakespan() - advancedMetrics.getMakespan()) / 
                                   baseMetrics.getMakespan();
        
        double waitTimeImprovement = 100 * (baseMetrics.getAverageWaitingTime() - 
                                          advancedMetrics.getAverageWaitingTime()) / 
                                   baseMetrics.getAverageWaitingTime();
        
        double turnaroundImprovement = 100 * (baseMetrics.getAverageTurnaroundTime() - 
                                            advancedMetrics.getAverageTurnaroundTime()) / 
                                     baseMetrics.getAverageTurnaroundTime();
        
        double utilizationImprovement = 100 * (advancedMetrics.getResourceUtilization() - 
                                             baseMetrics.getResourceUtilization()) / 
                                      baseMetrics.getResourceUtilization();
        
        System.out.printf("%s vs %s:\n", baseAlgorithm, advancedAlgorithm);
        System.out.printf("  Makespan: %.2f%% reduction\n", makespanImprovement);
        System.out.printf("  Average waiting time: %.2f%% reduction\n", waitTimeImprovement);
        System.out.printf("  Average turnaround time: %.2f%% reduction\n", turnaroundImprovement);
        System.out.printf("  Resource utilization: %.2f%% improvement\n", utilizationImprovement);
    }
}