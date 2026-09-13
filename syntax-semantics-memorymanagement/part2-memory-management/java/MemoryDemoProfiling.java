
import java.util.ArrayList;
import java.util.List;
 
public class MemoryDemoProfiling {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting -- attach jconsole/JMC now. Waiting 10s...");
        Thread.sleep(10_000);   // gives you time to open jconsole and connect
 
        List<int[]> buffers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            buffers.add(new int[1_000_000]);
        }
        System.out.println("Allocated 5 one-million-int buffers.");
        System.out.println("Watch the Memory tab now -- pausing 10s before clearing...");
        Thread.sleep(10_000);
 
        buffers.clear();
        buffers = null;
        System.gc();
        System.out.println("Cleared references and requested GC.");
        System.out.println("Watch the Memory tab now -- pausing 10s so you can see the drop...");
        Thread.sleep(10_000);
 
        System.out.println("Done.");
    }
}