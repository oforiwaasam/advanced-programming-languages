import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Entry point: wires the queue, writer and workers together, feeds in the
 * tasks, waits for everything to finish, and prints a summary that shows
 * no task was lost or duplicated.
 */
public final class Main {

    // Must run before any logger is used, or the one-line format is ignored.
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tT %4$s [%3$s] %5$s%n");
    }

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final int NUM_WORKERS = 4;
    private static final int NUM_TASKS = 10;
    private static final int BAD_TASK_ID = 5; // gets a null payload on purpose
    private static final String OUTPUT_FILE = "results.txt";
    private static final String LOG_FILE = "app.log";
    private static final long TIMEOUT_SECONDS = 30;
    private static final int PRODUCER_DELAY_MS = 200; // pause between adding tasks (demo of workers waiting)

    public static void main(String[] args) {
        setupLogging();

        TaskQueue taskQueue = new TaskQueue();

        // try-with-resources closes the writer automatically, even on error.
        // The constructor throws IOException if the file can't be opened.
        try (ResultsWriter resultsWriter = new ResultsWriter(OUTPUT_FILE)) {

            // Name pool threads Worker-1, Worker-2... so the queue's log lines
            // match the worker's own ("pool-1-thread-1" would not).
            AtomicInteger threadNumber = new AtomicInteger(1);
            ThreadFactory factory = r -> new Thread(r, "Worker-" + threadNumber.getAndIncrement());
            ExecutorService executor = Executors.newFixedThreadPool(NUM_WORKERS, factory);

            // Start the workers first; they block on the empty queue until
            // tasks arrive.
            for (int i = 1; i <= NUM_WORKERS; i++) {
                executor.submit(new Worker(i, taskQueue, resultsWriter));
            }

            for (int i = 1; i <= NUM_TASKS; i++) {
                String payload = (i == BAD_TASK_ID) ? null : "data-" + i;
                taskQueue.addTask(new Task(i, payload));
                try {
                    Thread.sleep(PRODUCER_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.warning("Main interrupted while adding tasks");
                    break;
                }
            }

            // Poison pills: one for each worker, so they all exit cleanly.
            for (int i = 0; i < NUM_WORKERS; i++) {
                taskQueue.addTask(Task.POISON);
            }

            // shutdown() only stops new submissions; it does not wait.
            executor.shutdown();
            try {
                if (!executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    LOGGER.severe("Timed out waiting for workers; forcing shutdown");
                    executor.shutdownNow(); // interrupts any stragglers
                    executor.awaitTermination(5, TimeUnit.SECONDS); // let interrupted workers finish logging
                }
            } catch (InterruptedException e) {
                LOGGER.warning("Main interrupted while waiting; forcing shutdown");
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            printSummary(resultsWriter);

        } catch (IOException e) {
            LOGGER.severe("File error, shutting down: " + e.getMessage());
        }
    }

    /** Compares what went in with what came out. */
    private static void printSummary(ResultsWriter resultsWriter) {
        int written = resultsWriter.getResultCount();
        int failed = NUM_TASKS - written;
        List<String> results = resultsWriter.getResults();

        LOGGER.info("---- Summary ----");
        LOGGER.info("Tasks added:     " + NUM_TASKS);
        LOGGER.info("Results written: " + written);
        LOGGER.info("Failed tasks:    " + failed);
        LOGGER.info(written + failed == NUM_TASKS
                ? "Check passed: every task accounted for"
                : "Check FAILED: tasks were lost or duplicated");
        results.forEach(r -> LOGGER.info("  " + r));
    }

    /** Sends logs to app.log as well as the console. */
    private static void setupLogging() {
        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE);
            fileHandler.setFormatter(new SimpleFormatter());
            Logger.getLogger("").addHandler(fileHandler); // root logger: covers every class
        } catch (IOException e) {
            // Not fatal: console logging still works.
            LOGGER.warning("Could not open log file, using console only: " + e.getMessage());
        }
    }
}
