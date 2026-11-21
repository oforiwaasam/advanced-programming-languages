/**
 * One unit of work for the data processing system.
 *
 * The class is final so nobody can subclass it. A subclass could add mutable
 * state (like a counter) and quietly break the guarantee that tasks are safe
 * to share between threads without locks.
 */
public final class Task {

    /**
     * Sentinel ("poison pill") that tells a worker to stop. It is static, so
     * there is exactly one in the whole program, created once when the class
     * loads. It is not stored inside each Task. Workers detect it by identity
     * (==), so no regular task can be mistaken for it, even one with id -1.
     * It is a Task so it can travel through the same queue as real work.
     */
    public static final Task POISON = new Task(-1, "POISON");

    private final int id;

    // String type keeps the payload flexible and easy to print in log lines.
    private final String payload;

    public Task(int id, String payload) {
        this.id = id;
        this.payload = payload;
    }

    // Getters only. No setters, since setters would defeat the final fields.
    public int getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    // Keeps the identity check in one place, so the worker loop can read
    // "if (task.isPoison()) break;" instead of comparing against POISON.
    public boolean isPoison() {
        return this == POISON;
    }

    @Override
    public String toString() {
        return "Task{id=" + id + ", payload='" + payload + "'}";
    }
}