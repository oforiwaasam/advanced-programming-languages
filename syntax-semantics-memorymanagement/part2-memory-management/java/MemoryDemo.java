
import java.util.ArrayList;
import java.util.List;
 
public class MemoryDemo {
    public static void main(String[] args) {
        List<int[]> buffers = new ArrayList<>();
 
        for (int i = 0; i < 5; i++) {
            buffers.add(new int[1_000_000]); // heap allocation via `new`;
                                              // the JVM manages the memory
        }
        System.out.println("Allocated " + buffers.size() + " one-million-int buffers");
        System.out.println("Free memory before clearing: "
                + Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
 
        buffers.clear();   // remove the strong references held in the list
        buffers = null;    // no strong references remain -> objects are now
                            // eligible for garbage collection
 
        System.gc();        // a REQUEST, not a guarantee, that GC runs now
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
 
        System.out.println("Free memory after GC request: "
                + Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
        System.out.println("Note: there is no way to dereference freed memory in Java."
                + " The language has no pointers, so dangling pointers cannot occur.");
    }
}
