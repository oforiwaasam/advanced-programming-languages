import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        final int NUM_WORKERS = 4;
        final int NUM_TASKS = 10;
        final String OUTPUT_FILE = "results.txt";

        TaskQueue taskQueue = new TaskQueue();
        ResultsWriter resultsWriter = new ResultsWriter(OUTPUT_FILE);
    }
}
