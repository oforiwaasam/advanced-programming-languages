import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Shared output for all worker threads. Each result is saved to a file and
 * also kept in memory so Main can print a summary at the end.
 *
 * This class has its own lock, separate from TaskQueue's. A worker never
 * holds both locks at once, which rules out deadlock between them.
 */
public final class ResultsWriter implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ResultsWriter.class.getName());

    // Neither the list nor the writer is thread-safe; the lock protects both.
    private final List<String> results = new ArrayList<>();
    private final BufferedWriter writer;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Opens (or overwrites) the output file.
     *
     * @param path location of the output file
     * @throws IOException if the file cannot be created or opened
     */
    public ResultsWriter(String path) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(path));
        LOGGER.info("Opened output file: " + path);
    }

    /**
     * Saves one result to the file and the in-memory list.
     *
     * @throws IOException if the file write fails; the caller decides how to react
     */
    public void writeResult(String result) throws IOException {
        Objects.requireNonNull(result, "result must not be null");

        lock.lock();
        try {
            writer.write(result);
            writer.newLine();
            writer.flush();

            results.add(result);
            LOGGER.fine("Saved result: " + result);
        } catch (IOException e) {
            LOGGER.severe(Thread.currentThread().getName()
                    + " failed to write result: " + e.getMessage());
            throw e; // let the worker know the write did not succeed
        } finally {
            lock.unlock(); // released even on failure, so other workers carry on
        }
    }

    public int getResultCount() {
        lock.lock();
        try {
            return results.size();
        } finally {
            lock.unlock();
        }
    }

    /** Returns a copy, so callers cannot modify the list without the lock. */
    public List<String> getResults() {
        lock.lock();
        try {
            return new ArrayList<>(results);
        } finally {
            lock.unlock();
        }
    }

    /** Flushes and closes the file. Called automatically by try-with-resources. */
    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            writer.close();
            LOGGER.info("Closed output file");
        } finally {
            lock.unlock();
        }
    }
}