# Employee Shift Scheduler (Java + JavaScript)

MSCS632 Advanced Programming Languages: control structures in two languages.

A company runs 7 days a week with three shifts a day (morning, afternoon, evening). The program collects each employee's ranked shift preferences, builds the weekly schedule, resolves conflicts, and prints the result.

| | Java | JavaScript |
|---|---|---|
| Paradigm / typing | Statically typed, class-based, compiled | Dynamically typed, functional/prototype-based, interpreted |
| Interface | Console (interactive prompts or `--demo`) | Browser GUI for input and output, plus `cli.js` for Node |
| Folder | `java/` | `javascript/` |

Both versions implement the same algorithm and use the same seeded random generator, so **the same input and seed produce byte-identical schedules in both languages**. That was used as a cross-check while building them.

## How to run

### Java (JDK 8 or newer)

```bash
cd java
javac *.java
java Main --demo            # built-in sample data of 12 employees
java Main                   # type in employees and preferences
java Main --demo --seed 7   # a different random top-up
java Main --demo --max 3    # tighter shift capacity
```

Interactive input: for each day, type the shifts in order of preference, e.g. `M E` (morning first, evening second). `M`, `A`, `E` or the full words both work. Leave a day blank if the employee does not want it. Finish the employee list with a blank name.

### JavaScript

Open `javascript/index.html` in any browser (no install or server needed).

1. Click **Load sample data** (or add employees and pick 1st/2nd/3rd choices per day).
2. Click **Generate schedule**.

Command line version with the same sample data (Node 14+):

```bash
node javascript/cli.js --seed 42
```

## Scheduling rules and how they are enforced

| Requirement | Where |
|---|---|
| Input and storage | Java: `Employee` holds a `List<List<Shift>>` (per day, ranked). JS: `{ name, prefs }` with `prefs[day]` an array of ranked shift indexes. |
| One shift per employee per day | `canWork()` refuses anyone who already has a shift that day |
| Max 5 days per week | `canWork()` also refuses anyone at 5 days (`daysWorked`) |
| At least 2 per shift per day | Phase 2 tops up each short shift with a **random** eligible employee (one with no shift that day and fewer than 5 days) |
| Shift conflicts | Preferred shift full → next ranked choice → any other shift with room that day → the next day (see below) |
| Readable output | Schedule by day, employee grid, preference summary, scheduling log, automatic checks |
| Bonus: ranked preferences | 1st choices for the whole day are served before any 2nd choice, then 3rd |
| Bonus: GUI | JavaScript browser GUI for input and output |

A shift holds at most **4** people by default (`--max N` / the "Max per shift" box). Without a cap a preferred shift could never be "full".

### Algorithm

1. **Preferences (per day, in rank rounds).** Everyone's 1st choice is tried first, then 2nd, then 3rd. The employee order rotates each day so nobody always goes first. A full shift is logged as `CONFLICT` and the employee moves on to their next choice (`RESOLVED`).
2. **Same-day fallback.** Anyone who asked for the day but whose ranked shifts are all full gets the least-filled shift with room (`REASSIGNED`). If they already work 5 days the request is `DROPPED`.
3. **Next-day carry-over.** If every shift that day is full, the employee is placed on the next day (`CARRIED`), after all employees' own requests have been served, so a carry-over never bumps someone's real request. Otherwise `UNPLACED`.
4. **Random top-up.** Any shift still below 2 gets random eligible employees (`RANDOM`). If nobody is eligible a `WARNING` is shown rather than breaking a rule.

Staffing math: 3 shifts × 2 people = 6 people a day, 42 assignments a week, at 5 days each that needs at least **9 employees**. With fewer the program still runs, flags the shortfall, and never violates the one-shift and 5-day rules.

The report ends with **checks computed from the finished schedule** (one shift per day, ≤ 5 days, ≥ 2 per shift, ≤ max per shift).

## Control structures used

| Structure | Java | JavaScript |
|---|---|---|
| `if` / `else if` / `else` | `Scheduler.build`, `Main.main` | `buildSchedule`, `app.js validate()` |
| `switch` | `Shift.parse`, option parsing in `Main` | `parseShift`, option parsing in `cli.js` |
| `for` (counted) | day / rank loops in `build` | day / rank loops in `buildSchedule` |
| Enhanced / for-of loops | `for (int e : order)`, `for (Shift s : shifts)` | `for (const e of order)`, `for (const [e, day] of unplaced)` |
| `while` | random top-up `while (size < MIN)`, interactive `while (true)` | top-up loop, option parsing loop in `cli.js` |
| `do … while` | re-prompt until a ranking is valid (`Main.collectInput`) | – (re-validation is event driven in the GUI) |
| `break` / `continue` | skipping ineligible employees, stopping when nobody is eligible | same |
| Ternary | report formatting | report formatting |
| Exceptions / guards | `try/catch` around number parsing | explicit validation with messages |

## Testing done

* Java and JavaScript output compared with `diff` for seeds 42, 7, 12345, 99999, -5 and capacities 4, 3, 2: identical every time.
* Edge cases: fewer than 9 employees (warnings, no rule violations), one employee requesting all 7 days (capped at 5, extra days `DROPPED`), a full day forcing a next-day carry-over.
* GUI: validation (empty list, blank and duplicate names, bad capacity), rank-select normalisation, light/dark theme, no horizontal page scroll at phone width.

## Files

```
java/Main.java        console input/output, sample data, option parsing
java/Scheduler.java   scheduling algorithm, report, checks, seeded RNG
java/Employee.java    employee + ranked preferences
java/Shift.java       shift enum and text parsing
javascript/scheduler.js   same algorithm as Scheduler.java (Node + browser)
javascript/app.js         GUI behaviour
javascript/index.html     GUI page and styles
javascript/cli.js         Node runner with the sample data
```
