import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

/**
 * One worker thread's job: repeatedly take a task from the shared queue,
 * process it, and save the result, until it receives the poison pill.
 *
 * The worker holds no locks of its own. TaskQueue and ResultsWriter are
 * already thread-safe, and the worker never holds both of their locks at
 * once, so it cannot cause a deadlock.
 */
public final class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final TaskQueue taskQueue;
    private final ResultsWriter resultsWriter;
    private final int workerId;
    private final Random random = new Random();

    public Worker(int workerId, TaskQueue taskQueue, ResultsWriter resultsWriter) {
        this.workerId = workerId;
        this.taskQueue = taskQueue;
        this.resultsWriter = resultsWriter;
    }

    @Override
    public void run() {
        String name = "Worker-" + workerId;
        int processed = 0;
        int failed = 0;
        LOGGER.info(name + " started");

        // Outer try: only InterruptedException belongs here, because an
        // interrupt means "stop", so it should end the whole loop.
        try {
            while (true) {
                Task task = taskQueue.getTask(); // blocks while the queue is empty

                if (task.isPoison()) {
                    LOGGER.info(name + " received poison pill, stopping");
                    break;
                }

                // Inner try: errors on ONE task must not kill the worker, so
                // this catch sits inside the loop and the loop continues.
                try {
                    String result = processTask(task);
                    resultsWriter.writeResult(result);
                    processed++;
                    LOGGER.info(name + " finished " + task);
                } catch (IOException e) {
                    failed++;
                    LOGGER.severe(name + " could not save result for task "
                            + task.getId() + ": " + e.getMessage());
                } catch (RuntimeException e) {
                    // Covers bad input (e.g. null payload) and any surprise.
                    // InterruptedException is checked, so it is not caught
                    // here and still reaches the outer catch.
                    failed++;
                    LOGGER.severe(name + " failed on task "
                            + task.getId() + ": " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the flag for the executor
            LOGGER.warning(name + " interrupted, stopping");
        } finally {
            // Runs however the loop ends, so every worker logs its exit.
            LOGGER.info(name + " completed (processed " + processed
                    + ", failed " + failed + ")");
        }
    }

    /**
     * Simulates work on one task and builds the result string. Only computes;
     * no locking or file access happens here.
     *
     * @throws InterruptedException if interrupted while "working"; passed up so
     *         run() can stop instead of recording a fake result
     * @throws IllegalArgumentException if the task has no payload
     */
    private String processTask(Task task) throws InterruptedException {
        if (task.getPayload() == null) {
            throw new IllegalArgumentException("task " + task.getId() + " has no payload");
        }

        Thread.sleep(100 + random.nextInt(400)); // 100-499 ms of pretend work

        return "Worker-" + workerId + " processed Task " + task.getId()
                + ": " + task.getPayload().toUpperCase();
    }
}