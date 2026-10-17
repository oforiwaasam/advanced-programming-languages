import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a weekly shift schedule.
 *
 * Rules enforced:
 *   - one shift per employee per day
 *   - at most MAX_DAYS (5) working days per employee per week
 *   - at least MIN_PER_SHIFT (2) employees on every shift of every day (random top-up)
 *   - at most maxPerShift employees on one shift, so a preferred shift can be "full"
 *
 * Bonus: preferences are ranked, and every employee's 1st choices are served before any
 * 2nd choice is considered.
 */
public class Scheduler {
    public static final int DAYS = 7;
    public static final int MAX_DAYS = 5;
    public static final int MIN_PER_SHIFT = 2;
    public static final String[] DAY_NAMES = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    /**
     * Small seeded random number generator (Mulberry32). It is implemented identically in the
     * JavaScript version, so the same seed produces the same schedule in both languages.
     */
    static class Rng {
        private int state;

        Rng(int seed) {
            state = seed;
        }

        double next() {
            state += 0x6D2B79F5;
            int t = (state ^ (state >>> 15)) * (1 | state);
            t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
            return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
        }
    }

    private final List<Employee> employees;
    private final int maxPerShift;
    private final int seed;
    private final Rng rng;
    private final Shift[] shifts = Shift.values();

    /** slots[day][shift] = indexes of the employees working that shift. */
    private final List<Integer>[][] slots;
    /** assigned[employee][day] = the shift that employee works that day, or null. */
    private final Shift[][] assigned;
    private final int[] daysWorked;
    private final List<String> log = new ArrayList<String>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Scheduler(List<Employee> employees, int maxPerShift, int seed) {
        this.employees = employees;
        this.maxPerShift = maxPerShift;
        this.seed = seed;
        this.rng = new Rng(seed);
        this.slots = new List[DAYS][shifts.length];
        for (int d = 0; d < DAYS; d++) {
            for (int s = 0; s < shifts.length; s++) {
                slots[d][s] = new ArrayList<Integer>();
            }
        }
        this.assigned = new Shift[employees.size()][DAYS];
        this.daysWorked = new int[employees.size()];
    }

    // ------------------------------------------------------------------ helpers

    private static String abbr(int day) {
        return DAY_NAMES[day].substring(0, 3);
    }

    /** True when the employee has no shift yet today and has not reached the weekly limit. */
    private boolean canWork(int e, int day) {
        return assigned[e][day] == null && daysWorked[e] < MAX_DAYS;
    }

    private boolean hasRoom(int day, Shift shift) {
        return slots[day][shift.ordinal()].size() < maxPerShift;
    }

    private void assign(int e, int day, Shift shift) {
        slots[day][shift.ordinal()].add(e);
        assigned[e][day] = shift;
        daysWorked[e]++;
    }

    private void note(String tag, String where, String message) {
        log.add(String.format("%-10s %s: %s", tag, where, message));
    }

    /** First ranked shift that still has room, otherwise the least-filled shift with room. */
    private Shift bestShiftFor(List<Shift> prefs, int day) {
        for (Shift s : prefs) {
            if (hasRoom(day, s)) {
                return s;
            }
        }
        Shift best = null;
        for (Shift s : shifts) {
            if (!hasRoom(day, s)) {
                continue;
            }
            if (best == null || slots[day][s.ordinal()].size() < slots[day][best.ordinal()].size()) {
                best = s;
            }
        }
        return best;
    }

    /** Rotates the starting employee each day so nobody always gets first pick. */
    private List<Integer> fairnessOrder(int day) {
        int n = employees.size();
        List<Integer> order = new ArrayList<Integer>();
        for (int k = 0; k < n; k++) {
            order.add((day % n + k) % n);
        }
        return order;
    }

    // ---------------------------------------------------------------- scheduling

    public void build() {
        int n = employees.size();
        if (n * MAX_DAYS < DAYS * shifts.length * MIN_PER_SHIFT) {
            note("WARNING", "Week", "Only " + n + " employees: covering " + shifts.length * MIN_PER_SHIFT
                + " slots a day at " + MAX_DAYS + " days each needs at least "
                + (int) Math.ceil(DAYS * shifts.length * MIN_PER_SHIFT / (double) MAX_DAYS) + " employees");
        }

        List<int[]> unplaced = new ArrayList<int[]>();

        // PHASE 1 - honour ranked preferences, day by day.
        for (int day = 0; day < DAYS; day++) {
            List<Integer> order = fairnessOrder(day);

            // Round 0 serves every 1st choice, round 1 every 2nd choice, round 2 every 3rd choice.
            for (int rank = 0; rank < shifts.length; rank++) {
                for (int e : order) {
                    List<Shift> prefs = employees.get(e).getPreferences(day);
                    if (rank >= prefs.size()) {
                        continue;
                    }
                    if (!canWork(e, day)) {
                        continue;
                    }
                    Shift wanted = prefs.get(rank);
                    if (hasRoom(day, wanted)) {
                        assign(e, day, wanted);
                        if (rank > 0) {
                            note("RESOLVED", abbr(day), employees.get(e).getName()
                                + " placed in choice #" + (rank + 1) + " (" + wanted.label() + ")");
                        }
                    } else {
                        note("CONFLICT", abbr(day), employees.get(e).getName()
                            + "'s choice #" + (rank + 1) + " (" + wanted.label() + ") is full");
                    }
                }
            }

            // Anyone who asked for this day but is still unplaced: try another shift the same day.
            for (int e : order) {
                if (!employees.get(e).requestsDay(day)) {
                    continue;
                }
                if (assigned[e][day] != null) {
                    continue;
                }
                if (daysWorked[e] >= MAX_DAYS) {
                    note("DROPPED", abbr(day), employees.get(e).getName()
                        + " requested this day but already works " + MAX_DAYS + " days");
                    continue;
                }
                Shift pick = bestShiftFor(employees.get(e).getPreferences(day), day);
                if (pick != null) {
                    assign(e, day, pick);
                    note("REASSIGNED", abbr(day), employees.get(e).getName()
                        + " moved to " + pick.label() + " (all ranked shifts full)");
                } else {
                    unplaced.add(new int[] {e, day});
                }
            }
        }

        // PHASE 1b - still unplaced: try the next day (after everyone's own requests were served).
        for (int[] u : unplaced) {
            int e = u[0];
            int day = u[1];
            int next = day + 1;
            boolean placed = false;
            if (next < DAYS && canWork(e, next)) {
                Shift pick = bestShiftFor(employees.get(e).getPreferences(day), next);
                if (pick != null) {
                    assign(e, next, pick);
                    note("CARRIED", abbr(day) + " -> " + abbr(next), employees.get(e).getName()
                        + " placed on " + abbr(next) + " " + pick.label());
                    placed = true;
                }
            }
            if (!placed) {
                note("UNPLACED", abbr(day), employees.get(e).getName()
                    + " could not be placed on " + abbr(day) + " or the next day");
            }
        }

        // PHASE 2 - top up any shift below the minimum with random eligible employees.
        for (int day = 0; day < DAYS; day++) {
            for (Shift shift : shifts) {
                while (slots[day][shift.ordinal()].size() < MIN_PER_SHIFT) {
                    List<Integer> pool = new ArrayList<Integer>();
                    for (int e = 0; e < n; e++) {
                        if (canWork(e, day)) {
                            pool.add(e);
                        }
                    }
                    if (pool.isEmpty()) {
                        note("WARNING", abbr(day), shift.label() + " is short-staffed and nobody is eligible to add");
                        break;
                    }
                    int chosen = pool.get((int) Math.floor(rng.next() * pool.size()));
                    assign(chosen, day, shift);
                    note("RANDOM", abbr(day), employees.get(chosen).getName() + " added to "
                        + shift.label() + " (below minimum of " + MIN_PER_SHIFT + ")");
                }
            }
        }
    }

    // -------------------------------------------------------------------- output

    private List<String> namesAt(int day, Shift shift) {
        List<Integer> ids = new ArrayList<Integer>(slots[day][shift.ordinal()]);
        Collections.sort(ids);
        List<String> names = new ArrayList<String>();
        for (int id : ids) {
            names.add(employees.get(id).getName());
        }
        return names;
    }

    private static String banner(String title) {
        return "==================== " + title + " ====================";
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private void appendCheck(StringBuilder sb, String tag, String text, List<String> details) {
        sb.append("  ").append(String.format("%-6s", tag)).append(" ").append(text).append("\n");
        for (String detail : details) {
            sb.append("         - ").append(detail).append("\n");
        }
    }

    public String report() {
        StringBuilder sb = new StringBuilder();
        int n = employees.size();
        int width = 4;
        for (Employee e : employees) {
            width = Math.max(width, e.getName().length());
        }

        sb.append("Random seed: ").append(seed).append("   Max per shift: ").append(maxPerShift).append("\n\n");

        // Weekly schedule
        sb.append(banner("WEEKLY SCHEDULE")).append("\n");
        for (int d = 0; d < DAYS; d++) {
            sb.append(DAY_NAMES[d]).append("\n");
            for (Shift s : shifts) {
                List<String> names = namesAt(d, s);
                sb.append(String.format("  %-9s (%d): %s\n", s.label(), names.size(), join(names)));
            }
            sb.append("\n");
        }

        // Employee view
        sb.append(banner("EMPLOYEE VIEW")).append("\n");
        sb.append(String.format("%-" + width + "s  ", "Name"));
        for (int d = 0; d < DAYS; d++) {
            sb.append(String.format("%-5s", abbr(d)));
        }
        sb.append("Days\n");
        for (int e = 0; e < n; e++) {
            sb.append(String.format("%-" + width + "s  ", employees.get(e).getName()));
            for (int d = 0; d < DAYS; d++) {
                String cell = assigned[e][d] == null ? "-" : String.valueOf(assigned[e][d].letter());
                sb.append(String.format("%-5s", cell));
            }
            sb.append(daysWorked[e]).append("\n");
        }
        sb.append("M = Morning, A = Afternoon, E = Evening, - = off\n\n");

        // Preference summary (bonus)
        int requested = 0;
        int[] choice = new int[shifts.length];
        int differentShift = 0;
        int notScheduled = 0;
        int unrequested = 0;
        for (int e = 0; e < n; e++) {
            for (int d = 0; d < DAYS; d++) {
                List<Shift> prefs = employees.get(e).getPreferences(d);
                if (prefs.isEmpty()) {
                    if (assigned[e][d] != null) {
                        unrequested++;
                    }
                    continue;
                }
                requested++;
                if (assigned[e][d] == null) {
                    notScheduled++;
                } else {
                    int rank = prefs.indexOf(assigned[e][d]);
                    if (rank >= 0) {
                        choice[rank]++;
                    } else {
                        differentShift++;
                    }
                }
            }
        }
        sb.append(banner("PREFERENCE SUMMARY")).append("\n");
        sb.append(String.format("  %-30s %d\n", "Days requested", requested));
        sb.append(String.format("  %-30s %d\n", "Got 1st choice", choice[0]));
        sb.append(String.format("  %-30s %d\n", "Got 2nd choice", choice[1]));
        sb.append(String.format("  %-30s %d\n", "Got 3rd choice", choice[2]));
        sb.append(String.format("  %-30s %d\n", "Different shift", differentShift));
        sb.append(String.format("  %-30s %d\n", "Requested but not scheduled", notScheduled));
        sb.append(String.format("  %-30s %d\n\n", "Added on days not requested", unrequested));

        // Log
        sb.append(banner("SCHEDULING LOG")).append("\n");
        if (log.isEmpty()) {
            sb.append("  (no conflicts or special assignments)\n");
        }
        for (String line : log) {
            sb.append("  ").append(line).append("\n");
        }
        sb.append("\n");

        // Checks computed from the finished schedule, independent of the scheduling code.
        sb.append(banner("CHECKS")).append("\n");
        List<String> oneShift = new ArrayList<String>();
        for (int d = 0; d < DAYS; d++) {
            int[] seen = new int[n];
            for (int s = 0; s < shifts.length; s++) {
                for (int e : slots[d][s]) {
                    seen[e]++;
                }
            }
            for (int e = 0; e < n; e++) {
                if (seen[e] > 1) {
                    oneShift.add(employees.get(e).getName() + " on " + abbr(d) + " works " + seen[e] + " shifts");
                }
            }
        }
        appendCheck(sb, oneShift.isEmpty() ? "[OK]" : "[FAIL]",
            "No employee works more than one shift per day", oneShift);

        List<String> tooManyDays = new ArrayList<String>();
        for (int e = 0; e < n; e++) {
            int days = 0;
            for (int d = 0; d < DAYS; d++) {
                boolean works = false;
                for (int s = 0; s < shifts.length; s++) {
                    if (slots[d][s].contains(e)) {
                        works = true;
                    }
                }
                if (works) {
                    days++;
                }
            }
            if (days > MAX_DAYS) {
                tooManyDays.add(employees.get(e).getName() + " works " + days + " days");
            }
        }
        appendCheck(sb, tooManyDays.isEmpty() ? "[OK]" : "[FAIL]",
            "No employee works more than " + MAX_DAYS + " days", tooManyDays);

        List<String> understaffed = new ArrayList<String>();
        List<String> overfull = new ArrayList<String>();
        for (int d = 0; d < DAYS; d++) {
            for (Shift s : shifts) {
                int size = slots[d][s.ordinal()].size();
                if (size < MIN_PER_SHIFT) {
                    understaffed.add(abbr(d) + " " + s.label() + " has " + size + " (minimum " + MIN_PER_SHIFT + ")");
                }
                if (size > maxPerShift) {
                    overfull.add(abbr(d) + " " + s.label() + " has " + size + " (max " + maxPerShift + ")");
                }
            }
        }
        appendCheck(sb, understaffed.isEmpty() ? "[OK]" : "[WARN]",
            "Every shift has at least " + MIN_PER_SHIFT + " employees", understaffed);
        appendCheck(sb, overfull.isEmpty() ? "[OK]" : "[FAIL]",
            "No shift exceeds " + maxPerShift + " employees", overfull);

        return sb.toString();
    }
}
