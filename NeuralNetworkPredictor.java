import java.util.*;
import java.io.*;
//import java.util.stream.Collectors;

/**
 * Neural Network based Job Runtime Predictor
 * 
 * This class provides deep learning-based job execution time prediction for improved
 * scheduling accuracy. It uses a simple feedforward neural network with one hidden layer.
 */
public class NeuralNetworkPredictor {
    // Network architecture
    private static final int INPUT_FEATURES = 7;  // Increased to include resource requirements
    private static final int HIDDEN_NEURONS = 10;
    private static final int OUTPUT_NEURONS = 1;
    
    // Network weights
    private double[][] weightsInputToHidden;
    private double[][] weightsHiddenToOutput;
    
    // Biases
    private double[] hiddenBiases;
    private double[] outputBiases;
    
    // Historical job data for training
    private List<JobHistoryRecord> jobHistory;
    
    // Learning parameters
    private double learningRate;
    private int epochs;
    
    // Feature normalization parameters
    private double[] featureMeans;
    private double[] featureStdDevs;
    
    // Performance metrics
    private double lastTrainingError;
    private double validationError;
    
    /**
     * Create a new Neural Network Runtime Predictor
     */
    public NeuralNetworkPredictor() {
        this.jobHistory = new ArrayList<>();
        this.learningRate = 0.01;
        this.epochs = 1000;
        this.featureMeans = new double[INPUT_FEATURES];
        this.featureStdDevs = new double[INPUT_FEATURES];
        
        // Initialize network weights with random values
        initializeNetwork();
    }
    
    /**
     * Initialize the neural network with random weights
     */
    private void initializeNetwork() {
        Random random = new Random(42);
        
        // Initialize weights with Xavier/Glorot initialization
        double inputScale = Math.sqrt(2.0 / (INPUT_FEATURES + HIDDEN_NEURONS));
        double hiddenScale = Math.sqrt(2.0 / (HIDDEN_NEURONS + OUTPUT_NEURONS));
        
        weightsInputToHidden = new double[INPUT_FEATURES][HIDDEN_NEURONS];
        for (int i = 0; i < INPUT_FEATURES; i++) {
            for (int h = 0; h < HIDDEN_NEURONS; h++) {
                weightsInputToHidden[i][h] = (random.nextDouble() * 2 - 1) * inputScale;
            }
        }
        
        weightsHiddenToOutput = new double[HIDDEN_NEURONS][OUTPUT_NEURONS];
        for (int h = 0; h < HIDDEN_NEURONS; h++) {
            for (int o = 0; o < OUTPUT_NEURONS; o++) {
                weightsHiddenToOutput[h][o] = (random.nextDouble() * 2 - 1) * hiddenScale;
            }
        }
        
        // Initialize biases
        hiddenBiases = new double[HIDDEN_NEURONS];
        outputBiases = new double[OUTPUT_NEURONS];
        
        for (int h = 0; h < HIDDEN_NEURONS; h++) {
            hiddenBiases[h] = (random.nextDouble() * 2 - 1) * inputScale;
        }
        
        for (int o = 0; o < OUTPUT_NEURONS; o++) {
            outputBiases[o] = (random.nextDouble() * 2 - 1) * hiddenScale;
        }
    }
    
    /**
     * Predict execution time for a job using the neural network
     * 
     * @param job The job to predict
     * @return Predicted execution time
     */
    public int predictExecutionTime(ResourceAwareJob job) {
        // If we don't have enough history, use the job's own estimate
        if (jobHistory.size() < 20) {
            return job.getEstimatedExecutionTime();
        }
        
        // Extract features
        double[] features = extractFeatures(job);
        
        // Normalize features
        normalizeFeatures(features);
        
        // Forward pass through the network
        double prediction = forwardPass(features);
        
        // Ensure prediction is positive
        int predictedTime = (int) Math.max(1, Math.round(prediction));
        
        return predictedTime;
    }
    
    /**
     * Perform a forward pass through the neural network
     * 
     * @param features Input features
     * @return Network output (predicted time)
     */
    private double forwardPass(double[] features) {
        // Calculate hidden layer activations
        double[] hiddenActivations = new double[HIDDEN_NEURONS];
        
        for (int h = 0; h < HIDDEN_NEURONS; h++) {
            double sum = hiddenBiases[h];
            for (int i = 0; i < INPUT_FEATURES; i++) {
                sum += features[i] * weightsInputToHidden[i][h];
            }
            hiddenActivations[h] = relu(sum);
        }
        
        // Calculate output layer activation
        double outputActivation = outputBiases[0];
        for (int h = 0; h < HIDDEN_NEURONS; h++) {
            outputActivation += hiddenActivations[h] * weightsHiddenToOutput[h][0];
        }
        
        // Linear activation for output (regression task)
        return outputActivation;
    }
    
    /**
     * ReLU activation function
     */
    private double relu(double x) {
        return Math.max(0, x);
    }
    
    /**
     * Derivative of ReLU activation function
     */
    private double reluDerivative(double x) {
        return x > 0 ? 1 : 0;
    }
    
    /**
     * Record actual execution time for a completed job for model improvement
     * 
     * @param job The completed job
     */
    public void recordJobCompletion(ResourceAwareJob job) {
        if (job.getCompletionTime() == -1 || job.getStartTime() == -1) {
            return; // Job not completed yet
        }
        
        int actualExecutionTime = job.getCompletionTime() - job.getStartTime();
        
        // Create historical record
        JobHistoryRecord record = new JobHistoryRecord(
            job.getId(),
            job.getEstimatedExecutionTime(),
            actualExecutionTime,
            job.getPriority(),
            job.isIOBound(),
            job.getDependencies().size(),
            job.getMemoryRequirement(),
            job.getNetworkRequirement()
        );
        
        // Add to history
        jobHistory.add(record);
        
        // Re-train model after collecting enough new data
        if (jobHistory.size() % 10 == 0) {
            trainNetwork();
        }
    }
    
    /**
     * Load historical job data from a CSV file
     * 
     * @param filePath Path to the CSV file
     */
    public void loadHistoricalData(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean header = true;
            
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                
                String[] parts = line.split(",");
                if (parts.length >= 8) {
                    try {
                        int jobId = Integer.parseInt(parts[0]);
                        int estimatedTime = Integer.parseInt(parts[1]);
                        int actualTime = Integer.parseInt(parts[2]);
                        int priority = Integer.parseInt(parts[3]);
                        boolean isIOBound = Boolean.parseBoolean(parts[4]);
                        int numDependencies = Integer.parseInt(parts[5]);
                        int memoryReq = Integer.parseInt(parts[6]);
                        int networkReq = Integer.parseInt(parts[7]);
                        
                        JobHistoryRecord record = new JobHistoryRecord(
                            jobId, estimatedTime, actualTime, priority, isIOBound, 
                            numDependencies, memoryReq, networkReq
                        );
                        
                        jobHistory.add(record);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing line: " + line);
                    }
                }
            }
            
            System.out.println("Loaded " + jobHistory.size() + " historical job records");
            
            // Train initial model
            if (jobHistory.size() >= 20) {
                trainNetwork();
            }
        } catch (IOException e) {
            System.err.println("Error loading historical data: " + e.getMessage());
        }
    }
    
    /**
     * Train the neural network using historical job data
     */
    private void trainNetwork() {
        // Skip if not enough data
        if (jobHistory.size() < 20) {
            return;
        }
        
        // Calculate feature statistics for normalization
        calculateFeatureStatistics();
        
        // Split data into training (80%) and validation (20%) sets
        List<JobHistoryRecord> shuffledData = new ArrayList<>(jobHistory);
        Collections.shuffle(shuffledData, new Random(42));
        
        int trainSize = (int)(shuffledData.size() * 0.8);
        List<JobHistoryRecord> trainingData = shuffledData.subList(0, trainSize);
        List<JobHistoryRecord> validationData = shuffledData.subList(trainSize, shuffledData.size());
        
        // Training loop
        double prevError = Double.MAX_VALUE;
        double currentError = 0;
        
        for (int epoch = 0; epoch < epochs; epoch++) {
            currentError = 0;
            
            // Process each training example
            for (JobHistoryRecord record : trainingData) {
                // Get features and target
                double[] features = extractFeatures(record);
                normalizeFeatures(features);
                double target = record.getActualTime();
                
                // Forward pass
                double[] hiddenActivations = new double[HIDDEN_NEURONS];
                double[] hiddenPreActivations = new double[HIDDEN_NEURONS];
                
                // Hidden layer
                for (int h = 0; h < HIDDEN_NEURONS; h++) {
                    hiddenPreActivations[h] = hiddenBiases[h];
                    for (int i = 0; i < INPUT_FEATURES; i++) {
                        hiddenPreActivations[h] += features[i] * weightsInputToHidden[i][h];
                    }
                    hiddenActivations[h] = relu(hiddenPreActivations[h]);
                }
                
                // Output layer
                double outputPreActivation = outputBiases[0];
                for (int h = 0; h < HIDDEN_NEURONS; h++) {
                    outputPreActivation += hiddenActivations[h] * weightsHiddenToOutput[h][0];
                }
                double prediction = outputPreActivation; // Linear activation
                
                // Calculate error
                double error = prediction - target;
                currentError += error * error;
                
                // Backpropagation
                // Output layer gradients
                double outputGradient = error;
                
                // Hidden layer gradients
                double[] hiddenGradients = new double[HIDDEN_NEURONS];
                for (int h = 0; h < HIDDEN_NEURONS; h++) {
                    hiddenGradients[h] = outputGradient * weightsHiddenToOutput[h][0] * 
                                         reluDerivative(hiddenPreActivations[h]);
                }
                
                // Update weights and biases
                // Output layer updates
                for (int h = 0; h < HIDDEN_NEURONS; h++) {
                    weightsHiddenToOutput[h][0] -= learningRate * outputGradient * hiddenActivations[h];
                }
                outputBiases[0] -= learningRate * outputGradient;
                
                // Hidden layer updates
                for (int i = 0; i < INPUT_FEATURES; i++) {
                    for (int h = 0; h < HIDDEN_NEURONS; h++) {
                        weightsInputToHidden[i][h] -= learningRate * hiddenGradients[h] * features[i];
                    }
                }
                
                for (int h = 0; h < HIDDEN_NEURONS; h++) {
                    hiddenBiases[h] -= learningRate * hiddenGradients[h];
                }
            }
            
            // Calculate mean squared error
            currentError /= trainingData.size();
            
            // Early stopping if error change is very small
            if (Math.abs(prevError - currentError) < 0.0001) {
                break;
            }
            
            prevError = currentError;
            
            // Adjust learning rate (learning rate decay)
            if (epoch % 100 == 0) {
                learningRate *= 0.9;
            }
        }
        
        // Store final training error
        lastTrainingError = currentError;
        
        // Calculate validation error
        validationError = calculateValidationError(validationData);
        
        System.out.printf("Neural network trained. Training MSE: %.4f, Validation MSE: %.4f%n", 
                         lastTrainingError, validationError);
    }
    
    /**
     * Calculate validation error on a separate dataset
     */
    private double calculateValidationError(List<JobHistoryRecord> validationData) {
        if (validationData.isEmpty()) {
            return Double.NaN;
        }
        
        double sumSquaredError = 0;
        
        for (JobHistoryRecord record : validationData) {
            double[] features = extractFeatures(record);
            normalizeFeatures(features);
            double target = record.getActualTime();
            
            double prediction = forwardPass(features);
            double error = prediction - target;
            sumSquaredError += error * error;
        }
        
        return sumSquaredError / validationData.size();
    }
    
    /**
     * Extract features from a job for prediction
     * 
     * @param job The job to extract features from
     * @return Array of feature values
     */
    private double[] extractFeatures(ResourceAwareJob job) {
        double[] features = new double[INPUT_FEATURES];
        
        // Feature 1: Estimated execution time
        features[0] = job.getEstimatedExecutionTime();
        
        // Feature 2: Priority
        features[1] = job.getPriority();
        
        // Feature 3: Is I/O bound
        features[2] = job.isIOBound() ? 1.0 : 0.0;
        
        // Feature 4: Number of dependencies
        features[3] = job.getDependencies().size();
        
        // Feature 5: Memory requirement
        features[4] = job.getMemoryRequirement();
        
        // Feature 6: Network requirement
        features[5] = job.getNetworkRequirement();
        
        // Feature 7: Custom feature (e.g., arrival time)
        features[6] = job.getArrivalTime();
        
        return features;
    }
    
    /**
     * Extract features from a job history record
     */
    private double[] extractFeatures(JobHistoryRecord record) {
        double[] features = new double[INPUT_FEATURES];
        
        features[0] = record.getEstimatedTime();
        features[1] = record.getPriority();
        features[2] = record.isIOBound() ? 1.0 : 0.0;
        features[3] = record.getNumDependencies();
        features[4] = record.getMemoryRequirement();
        features[5] = record.getNetworkRequirement();
        features[6] = 0; // Arrival time not stored in history
        
        return features;
    }
    
    /**
     * Normalize features using z-score normalization
     * 
     * @param features Features to normalize (in-place)
     */
    private void normalizeFeatures(double[] features) {
        if (jobHistory.size() < 20) {
            return; // Not enough data for normalization
        }
        
        for (int i = 0; i < INPUT_FEATURES; i++) {
            features[i] = (features[i] - featureMeans[i]) / featureStdDevs[i];
            
            // Handle division by zero
            if (Double.isNaN(features[i]) || Double.isInfinite(features[i])) {
                features[i] = 0.0;
            }
        }
    }
    
    /**
     * Calculate feature means and standard deviations for normalization
     */
    private void calculateFeatureStatistics() {
        // Skip if not enough data
        if (jobHistory.size() < 20) {
            return;
        }
        
        // Initialize arrays
        double[] sums = new double[INPUT_FEATURES];
        double[] sumSquares = new double[INPUT_FEATURES];
        
        // Calculate sums
        for (JobHistoryRecord record : jobHistory) {
            double[] features = extractFeatures(record);
            
            for (int i = 0; i < INPUT_FEATURES; i++) {
                sums[i] += features[i];
                sumSquares[i] += features[i] * features[i];
            }
        }
        
        // Calculate means and standard deviations
        int n = jobHistory.size();
        for (int i = 0; i < INPUT_FEATURES; i++) {
            featureMeans[i] = sums[i] / n;
            featureStdDevs[i] = Math.sqrt((sumSquares[i] / n) - (featureMeans[i] * featureMeans[i]));
            
            // Handle zero standard deviation
            if (featureStdDevs[i] < 0.0001) {
                featureStdDevs[i] = 1.0;
            }
        }
    }
    
    /**
     * Generate a report on prediction accuracy
     * 
     * @return String containing the report
     */
    public String generateAccuracyReport() {
        if (jobHistory.size() < 20) {
            return "Not enough data for accuracy report (need at least 20 job records)";
        }
        
        // Calculate various metrics
        double rmse = Math.sqrt(validationError);
        
        // Calculate mean absolute percentage error
        double sumAbsPercentageError = 0;
        int count = 0;
        
        // Use 5-fold cross validation for more robust metrics
        List<JobHistoryRecord> shuffledData = new ArrayList<>(jobHistory);
        Collections.shuffle(shuffledData, new Random(42));
        
        int foldSize = shuffledData.size() / 5;
        double[] foldMAPE = new double[5];
        
        for (int fold = 0; fold < 5; fold++) {
            int validationStart = fold * foldSize;
            int validationEnd = Math.min(shuffledData.size(), (fold + 1) * foldSize);
            
            List<JobHistoryRecord> validationFold = shuffledData.subList(validationStart, validationEnd);
            
            double foldSumAPE = 0;
            int foldCount = 0;
            
            for (JobHistoryRecord record : validationFold) {
                double[] features = extractFeatures(record);
                normalizeFeatures(features);
                double prediction = forwardPass(features);
                
                if (record.getActualTime() > 0) {
                    double absolutePercentageError = Math.abs((prediction - record.getActualTime()) / record.getActualTime()) * 100;
                    foldSumAPE += absolutePercentageError;
                    foldCount++;
                    
                    sumAbsPercentageError += absolutePercentageError;
                    count++;
                }
            }
            
            foldMAPE[fold] = foldCount > 0 ? foldSumAPE / foldCount : 0;
        }
        
        double mape = count > 0 ? sumAbsPercentageError / count : 0;
        double averageFoldMAPE = Arrays.stream(foldMAPE).average().orElse(0);
        
        // Calculate improvement over naive estimation
        double improvementOverNaive = calculateImprovementOverNaive();
        
        // Build report
        StringBuilder report = new StringBuilder();
        report.append("Neural Network Runtime Predictor Accuracy Report\n");
        report.append("==============================================\n");
        report.append(String.format("Number of job records: %d\n", jobHistory.size()));
        report.append(String.format("Neural network architecture: %d-%d-%d\n", INPUT_FEATURES, HIDDEN_NEURONS, OUTPUT_NEURONS));
        report.append(String.format("Training MSE: %.4f\n", lastTrainingError));
        report.append(String.format("Validation MSE: %.4f\n", validationError));
        report.append(String.format("Root Mean Squared Error (RMSE): %.2f\n", rmse));
        report.append(String.format("Mean Absolute Percentage Error (MAPE): %.2f%%\n", mape));
        report.append(String.format("Cross-validated MAPE: %.2f%%\n", averageFoldMAPE));
        report.append(String.format("Improvement over naive estimation: %.2f%%\n", improvementOverNaive));
        
        report.append("\nFeature Importance:\n");
        
        // Calculate feature importance using sensitivity analysis
        double[] importance = calculateFeatureImportance();
        String[] featureNames = {
            "Estimated Execution Time",
            "Priority",
            "I/O Bound Flag",
            "Number of Dependencies",
            "Memory Requirement",
            "Network Requirement",
            "Arrival Time"
        };
        
        for (int i = 0; i < INPUT_FEATURES; i++) {
            report.append(String.format("  %s: %.2f%%\n", featureNames[i], importance[i] * 100));
        }
        
        return report.toString();
    }
    
    /**
     * Calculate feature importance using sensitivity analysis
     */
    private double[] calculateFeatureImportance() {
        double[] importance = new double[INPUT_FEATURES];
        double[] baselineFeatures = new double[INPUT_FEATURES];
        
        // Create baseline features (averages)
        for (int i = 0; i < INPUT_FEATURES; i++) {
            baselineFeatures[i] = featureMeans[i];
        }
        
        // Normalize baseline features
        double[] normalizedBaseline = baselineFeatures.clone();
        normalizeFeatures(normalizedBaseline);
        
        // Get baseline prediction
        double baselinePrediction = forwardPass(normalizedBaseline);
        
        // Calculate sensitivity for each feature
        double totalSensitivity = 0;
        
        for (int i = 0; i < INPUT_FEATURES; i++) {
            // Create perturbed features
            double[] perturbedFeatures = normalizedBaseline.clone();
            perturbedFeatures[i] += 1.0; // Perturb by 1 standard deviation
            
            // Get prediction with perturbed feature
            double perturbedPrediction = forwardPass(perturbedFeatures);
            
            // Calculate sensitivity
            double sensitivity = Math.abs(perturbedPrediction - baselinePrediction);
            importance[i] = sensitivity;
            totalSensitivity += sensitivity;
        }
        
        // Normalize importance
        if (totalSensitivity > 0) {
            for (int i = 0; i < INPUT_FEATURES; i++) {
                importance[i] /= totalSensitivity;
            }
        }
        
        return importance;
    }
    
    /**
     * Calculate improvement over naive estimation
     * 
     * @return Percentage improvement
     */
    private double calculateImprovementOverNaive() {
        if (jobHistory.size() < 20) {
            return 0;
        }
        
        double mlErrorSum = 0;
        double naiveErrorSum = 0;
        
        // Use cross-validation
        List<JobHistoryRecord> shuffledData = new ArrayList<>(jobHistory);
        Collections.shuffle(shuffledData, new Random(42));
        
        int testSize = shuffledData.size() / 5; // 20% for testing
        List<JobHistoryRecord> testingData = shuffledData.subList(0, testSize);
        
        for (JobHistoryRecord record : testingData) {
            double[] features = extractFeatures(record);
            normalizeFeatures(features);
            double mlPrediction = forwardPass(features);
            
            // Calculate errors
            double mlError = Math.abs(mlPrediction - record.getActualTime());
            double naiveError = Math.abs(record.getEstimatedTime() - record.getActualTime());
            
            mlErrorSum += mlError;
            naiveErrorSum += naiveError;
        }
        
        // Calculate percentage improvement
        if (naiveErrorSum <= 0) {
            return 0;
        }
        
        return 100 * (naiveErrorSum - mlErrorSum) / naiveErrorSum;
    }
    
    /**
     * Set neural network hyperparameters
     * 
     * @param learningRate Learning rate for gradient descent
     * @param epochs Maximum number of training epochs
     */
    public void setHyperparameters(double learningRate, int epochs) {
        this.learningRate = learningRate;
        this.epochs = epochs;
    }
}