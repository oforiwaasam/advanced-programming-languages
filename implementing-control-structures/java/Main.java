import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/** Console front end for the employee shift scheduler. */
public class Main {

    public static void main(String[] args) {
        boolean demo = false;
        Integer seed = null;
        int maxPerShift = 4;

        // Command-line options (for loop + switch).
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--demo":
                        demo = true;
                        break;
                    case "--seed":
                        seed = Integer.parseInt(args[++i]);
                        break;
                    case "--max":
                        maxPerShift = Integer.parseInt(args[++i]);
                        break;
                    default:
                        System.err.println("Unknown option: " + args[i]);
                        printUsage();
                        return;
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            System.err.println("--seed and --max need a whole number.");
            printUsage();
            return;
        }

        if (maxPerShift < Scheduler.MIN_PER_SHIFT) {
            System.err.println("--max must be at least " + Scheduler.MIN_PER_SHIFT + ".");
            return;
        }

        List<Employee> employees = demo ? sampleEmployees() : collectInput(new Scanner(System.in));
        if (employees.isEmpty()) {
            System.out.println("No employees entered, nothing to schedule.");
            return;
        }

        if (seed == null) {
            seed = demo ? 42 : (int) (System.nanoTime() & 0x7FFFFFFF);
        }

        Scheduler scheduler = new Scheduler(employees, maxPerShift, seed);
        scheduler.build();
        System.out.println();
        System.out.print(scheduler.report());
    }

    private static void printUsage() {
        System.err.println("Usage: java Main [--demo] [--seed N] [--max N]");
        System.err.println("  (no options)  enter employees and preferences interactively");
        System.err.println("  --demo        use built-in sample data");
        System.err.println("  --seed N      seed for the random top-up (same seed = same schedule)");
        System.err.println("  --max N       maximum employees on one shift (default 4)");
    }

    // -------------------------------------------------------------------- input

    /** Turns "M E A" (or "morning, evening") into a ranked list. Returns null if invalid. */
    static List<Shift> parseRanking(String text) {
        List<Shift> ranked = new ArrayList<Shift>();
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return ranked;
        }
        for (String token : trimmed.split("[\\s,]+")) {
            Shift shift = Shift.parse(token);
            if (shift == null || ranked.contains(shift)) {
                return null;
            }
            ranked.add(shift);
        }
        return ranked;
    }

    private static List<Employee> collectInput(Scanner in) {
        List<Employee> employees = new ArrayList<Employee>();
        Set<String> usedNames = new HashSet<String>();

        System.out.println("Employee Shift Scheduler");
        System.out.println("Rank shifts from most to least preferred: M = Morning, A = Afternoon, E = Evening.");
        System.out.println("Example: \"M E\" means morning first, evening second. Leave a day blank to skip it.");
        System.out.println();

        while (true) {
            System.out.print("Employee name (blank to finish): ");
            if (!in.hasNextLine()) {
                break;
            }
            String name = in.nextLine().trim();
            if (name.isEmpty()) {
                break;
            }
            if (!usedNames.add(name.toLowerCase())) {
                System.out.println("  ! " + name + " was already entered.");
                continue;
            }

            Employee employee = new Employee(name);
            for (int day = 0; day < Scheduler.DAYS; day++) {
                List<Shift> ranked;
                boolean valid;
                do {
                    System.out.print("  " + Scheduler.DAY_NAMES[day] + " ranking: ");
                    String line = in.hasNextLine() ? in.nextLine() : "";
                    ranked = parseRanking(line);
                    valid = ranked != null;
                    if (!valid) {
                        System.out.println("  ! Use M, A, E without repeats, e.g. \"M E\", or leave blank.");
                    }
                } while (!valid);
                employee.setPreferences(day, ranked);
            }
            employees.add(employee);
        }
        return employees;
    }

    // --------------------------------------------------------------- sample data

    /** Same table as the JavaScript version. One string per day, Monday to Sunday. */
    private static final String[][] SAMPLE = {
        {"Alice",  "M A", "M",   "M E", "",    "M",   "",    ""},
        {"Bob",    "M E", "M A", "",    "A M", "",    "E",   ""},
        {"Carlos", "M",   "",    "M A", "M",   "A E", "M E", ""},
        {"Dana",   "M A E", "A", "",    "E A", "E",   "",    "M"},
        {"Elena",  "",    "E A", "E",   "",    "M A", "A E", "E"},
        {"Farid",  "M A", "M",   "A M", "M E", "",    "",    "A M"},
        {"Grace",  "",    "",    "A",   "A E", "A M", "E A", ""},
        {"Hiro",   "M",   "E M", "",    "",    "E",   "M",   "E A"},
        {"Imani",  "",    "A E", "M E", "A",   "",    "A",   "M E"},
        {"Jonas",  "A M", "",    "E",   "E M", "A",   "",    "A"},
        {"Kofi",   "",    "",    "M A", "",    "",    "A",   ""},
        {"Lena",   "",    "A",   "",    "E",   "M",   "",    "M A"},
    };

    private static List<Employee> sampleEmployees() {
        List<Employee> employees = new ArrayList<Employee>();
        for (String[] row : SAMPLE) {
            Employee employee = new Employee(row[0]);
            for (int day = 0; day < Scheduler.DAYS; day++) {
                employee.setPreferences(day, parseRanking(row[day + 1]));
            }
            employees.add(employee);
        }
        return employees;
    }
}
