public class Task {

    private final int taskId;
    private final String payload;

    public Task(int taskId, String payload) {
        this.taskId = taskId;
        this.payload = payload;
    }

    public int getTaskId() {
        return taskId;
    }

    public String getPayload() {
        return payload;
    }

    public String toString() {
        return "Task{id=" + taskId + ", payload='" + payload + "'}";
    }
    
}
