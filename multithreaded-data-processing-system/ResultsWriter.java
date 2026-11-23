import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class ResultsWriter {
    
    private final List<String> results = new ArrayList<>();
    private final BufferedWriter writer;
    private final ReentrantLock lock = new ReentrantLock();
    private static final Logger LOGGER = Logger.getLogger(ResultsWriter.class.getName());

    public ResultsWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public void writeResult(String result) throws IOException {
        lock.lock();
        try {
            results.add(result);
            writer.write(result);
            writer.newLine();
            writer.flush();
        } catch (Exception e) {
            LOGGER.severe("Error writing result: " + e.getMessage());
        } finally {
            lock.unlock();
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

    public List<String> getResults() {
        lock.lock();
        try {
            return new ArrayList<>(results);
        } finally {
            lock.unlock();
        }
    }

    public void close() throws IOException {
        lock.lock();
        try {
            writer.close();
        } finally {
            lock.unlock();
        }
    }
}
