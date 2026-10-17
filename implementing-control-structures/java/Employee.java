import java.util.ArrayList;
import java.util.List;

/**
 * An employee and their ranked shift preferences for each day of the week.
 *
 * Data structure: preferences.get(day) is an ordered list of shifts, where index 0 is the
 * first choice, index 1 the second choice and so on. An empty list means the employee did
 * not ask to work that day.
 */
public class Employee {
    private final String name;
    private final List<List<Shift>> preferences = new ArrayList<List<Shift>>();

    public Employee(String name) {
        this.name = name;
        for (int day = 0; day < Scheduler.DAYS; day++) {
            preferences.add(new ArrayList<Shift>());
        }
    }

    public String getName() {
        return name;
    }

    public void setPreferences(int day, List<Shift> ranked) {
        preferences.set(day, new ArrayList<Shift>(ranked));
    }

    public List<Shift> getPreferences(int day) {
        return preferences.get(day);
    }

    public boolean requestsDay(int day) {
        return !preferences.get(day).isEmpty();
    }
}
