import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final TaskQueue taskQueue;
    private final ResultsWriter resultsWriter;
    private final int workerId;
    private final Random random = new Random();

    public Worker(TaskQueue taskQueue, ResultsWriter resultsWriter) {
        this.taskQueue = taskQueue;
        this.resultsWriter = resultsWriter;
    }

    @Override
    public void run() {
        LOGGER.info(Thread.currentThread().getName() + " started");
        try {
            while (true) {
                Task task = taskQueue.getTask();
                if (task == Task.POISON) {
                    LOGGER.info(Thread.currentThread().getName() + " received poison pill, stopping");
                    break; // exit the loop and finish the thread
                }
                String result = processTask(task);
                resultsWriter.writeResult(result);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted status
            LOGGER.warning(Thread.currentThread().getName() + " interrupted, stopping");
        } catch (IOException e) {
            LOGGER.severe(Thread.currentThread().getName() + " failed to write result: " + e.getMessage());
        } finally {
            LOGGER.info(Thread.currentThread().getName() + " completed");
        }
    }

    private String processTask(Task task) {
        // Simulate some processing time
        try {
            Thread.sleep(random.nextInt(100));
            String processedPayload = task.getPayload().toUpperCase(); // Example processing: convert payload to uppercase
            return "Processed " + processedPayload + " by worker " + workerId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupted status
        }
        return "Failed to process " + task.getPayload() + " by worker " + workerId;
    }
    
}
