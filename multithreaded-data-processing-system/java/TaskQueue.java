import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class TaskQueue {
    
    private final Queue<Task> queue = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private static final Logger LOGGER = Logger.getLogger(TaskQueue.class.getName());

    public void addTask(Task task) {
        lock.lock();
        try {
            queue.add(task);
            notEmpty.signal(); // Notify a waiting thread that a new task is available
        } finally {
            lock.unlock();
        }
    }

    public Task getTask() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                LOGGER.info(Thread.currentThread().getName() + " waiting: queue empty");
                notEmpty.await(); // Wait until a task is available
            }
            return queue.poll();
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}
