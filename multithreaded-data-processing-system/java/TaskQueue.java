import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * A thread-safe FIFO queue of tasks shared between the producer (Main) and
 * the worker threads.
 *
 * The underlying LinkedList is not thread-safe, so every access goes through
 * a single ReentrantLock. Workers that find the queue empty wait on a
 * Condition instead of busy-polling, which keeps them from wasting CPU.
 */
public final class TaskQueue {

    private static final Logger LOGGER = Logger.getLogger(TaskQueue.class.getName());

    private final Queue<Task> queue = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    /**
     * Adds a task to the back of the queue and wakes one waiting worker.
     *
     * @param task the task to enqueue; must not be null
     * @throws NullPointerException if task is null
     */
    public void addTask(Task task) {
        // Reject null early: poll() also returns null, so a stored null
        // could not be told apart from "queue is empty".
        Objects.requireNonNull(task, "task must not be null");

        lock.lock();
        try {
            queue.add(task);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes and returns the task at the front of the queue, blocking until
     * one is available.
     *
     * @return the next task (possibly Task.POISON, which tells the worker to stop)
     * @throws InterruptedException if the thread is interrupted while waiting;
     *         the calling worker decides how to shut down
     */
    public Task getTask() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                LOGGER.info(Thread.currentThread().getName() + " waiting: queue empty");
                notEmpty.await(); // releases the lock while waiting
            }
            return queue.poll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reports whether the queue is empty at this instant. The answer can be
     * stale as soon as it returns, so use it only for logging or summaries,
     * never to decide whether to call getTask().
     */
    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}