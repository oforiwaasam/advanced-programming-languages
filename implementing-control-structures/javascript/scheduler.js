/*
 * Employee Shift Scheduler - core logic (JavaScript).
 *
 * Works in Node.js (module.exports) and in the browser (window.Scheduler).
 * It follows the same algorithm, log messages and report layout as the Java version, and uses
 * the same seeded random generator, so one seed gives the same schedule in both languages.
 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.Scheduler = factory();
  }
})(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  const SHIFTS = ['Morning', 'Afternoon', 'Evening'];
  const SHIFT_LETTERS = ['M', 'A', 'E'];
  const MAX_DAYS = 5;
  const MIN_PER_SHIFT = 2;
  const DEFAULT_MAX_PER_SHIFT = 4;

  // ---------------------------------------------------------------- parsing

  /** "m", "Morning", "2", "eve" -> 0, 1 or 2. Returns -1 when not recognised. */
  function parseShift(text) {
    // Branching with switch: several spellings map onto the same shift.
    switch (String(text).trim().toLowerCase()) {
      case 'm':
      case 'morning':
      case '1':
        return 0;
      case 'a':
      case 'afternoon':
      case '2':
        return 1;
      case 'e':
      case 'eve':
      case 'evening':
      case '3':
        return 2;
      default:
        return -1;
    }
  }

  /** "M E A" or "morning, evening" -> [0, 2, 1]. Blank -> []. Returns null when invalid. */
  function parseRanking(text) {
    const trimmed = String(text).trim();
    if (trimmed === '') {
      return [];
    }
    const ranked = [];
    for (const token of trimmed.split(/[\s,]+/)) {
      const shift = parseShift(token);
      if (shift === -1 || ranked.includes(shift)) {
        return null;
      }
      ranked.push(shift);
    }
    return ranked;
  }

  // ------------------------------------------------------------ random numbers

  /** Mulberry32: tiny seeded generator, identical to the Java implementation. */
  function mulberry32(seed) {
    let a = seed | 0;
    return function () {
      a = (a + 0x6d2b79f5) | 0;
      let t = Math.imul(a ^ (a >>> 15), 1 | a);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  // ------------------------------------------------------------- sample data

  /** One string per day, Monday to Sunday. Same table as the Java version. */
  const SAMPLE = [
    ['Alice',  'M A', 'M',   'M E', '',    'M',   '',    ''],
    ['Bob',    'M E', 'M A', '',    'A M', '',    'E',   ''],
    ['Carlos', 'M',   '',    'M A', 'M',   'A E', 'M E', ''],
    ['Dana',   'M A E', 'A', '',    'E A', 'E',   '',    'M'],
    ['Elena',  '',    'E A', 'E',   '',    'M A', 'A E', 'E'],
    ['Farid',  'M A', 'M',   'A M', 'M E', '',    '',    'A M'],
    ['Grace',  '',    '',    'A',   'A E', 'A M', 'E A', ''],
    ['Hiro',   'M',   'E M', '',    '',    'E',   'M',   'E A'],
    ['Imani',  '',    'A E', 'M E', 'A',   '',    'A',   'M E'],
    ['Jonas',  'A M', '',    'E',   'E M', 'A',   '',    'A'],
    ['Kofi',   '',    '',    'M A', '',    '',    'A',   ''],
    ['Lena',   '',    'A',   '',    'E',   'M',   '',    'M A'],
  ];

  function sampleEmployees() {
    return SAMPLE.map((row) => ({
      name: row[0],
      prefs: row.slice(1).map(parseRanking),
    }));
  }

  // -------------------------------------------------------------- scheduling

  /**
   * employees: [{ name, prefs }] where prefs[day] is a ranked array of shift indexes
   * (0 = Morning, 1 = Afternoon, 2 = Evening); an empty array means "not requested".
   * options:   { maxPerShift, seed }
   */
  function buildSchedule(employees, options) {
    const opts = options || {};
    const maxPerShift = opts.maxPerShift === undefined ? DEFAULT_MAX_PER_SHIFT : opts.maxPerShift;
    const seed = opts.seed === undefined ? 42 : opts.seed;
    const rand = mulberry32(seed);
    const n = employees.length;

    // slots[day][shift] = employee indexes; assigned[e][day] = shift index or -1.
    const slots = DAYS.map(() => SHIFTS.map(() => []));
    const assigned = employees.map(() => new Array(DAYS.length).fill(-1));
    const daysWorked = new Array(n).fill(0);
    const log = [];

    const abbr = (day) => DAYS[day].slice(0, 3);
    const canWork = (e, day) => assigned[e][day] === -1 && daysWorked[e] < MAX_DAYS;
    const hasRoom = (day, shift) => slots[day][shift].length < maxPerShift;

    function assign(e, day, shift) {
      slots[day][shift].push(e);
      assigned[e][day] = shift;
      daysWorked[e] += 1;
    }

    function note(tag, where, message) {
      log.push(tag.padEnd(10) + ' ' + where + ': ' + message);
    }

    /** First ranked shift that still has room, otherwise the least-filled shift with room. */
    function bestShiftFor(prefs, day) {
      for (const s of prefs) {
        if (hasRoom(day, s)) {
          return s;
        }
      }
      let best = -1;
      for (let s = 0; s < SHIFTS.length; s++) {
        if (!hasRoom(day, s)) {
          continue;
        }
        if (best === -1 || slots[day][s].length < slots[day][best].length) {
          best = s;
        }
      }
      return best;
    }

    /** Rotates the starting employee each day so nobody always gets first pick. */
    function fairnessOrder(day) {
      const order = [];
      for (let k = 0; k < n; k++) {
        order.push(((day % n) + k) % n);
      }
      return order;
    }

    const slotsNeeded = SHIFTS.length * MIN_PER_SHIFT;
    if (n * MAX_DAYS < DAYS.length * slotsNeeded) {
      note('WARNING', 'Week', 'Only ' + n + ' employees: covering ' + slotsNeeded + ' slots a day at ' +
        MAX_DAYS + ' days each needs at least ' + Math.ceil((DAYS.length * slotsNeeded) / MAX_DAYS) + ' employees');
    }

    const unplaced = [];

    // PHASE 1 - honour ranked preferences, day by day.
    for (let day = 0; day < DAYS.length; day++) {
      const order = fairnessOrder(day);

      // Round 0 serves every 1st choice, round 1 every 2nd choice, round 2 every 3rd choice.
      for (let rank = 0; rank < SHIFTS.length; rank++) {
        for (const e of order) {
          const prefs = employees[e].prefs[day];
          if (rank >= prefs.length) {
            continue;
          }
          if (!canWork(e, day)) {
            continue;
          }
          const wanted = prefs[rank];
          if (hasRoom(day, wanted)) {
            assign(e, day, wanted);
            if (rank > 0) {
              note('RESOLVED', abbr(day), employees[e].name + ' placed in choice #' + (rank + 1) +
                ' (' + SHIFTS[wanted] + ')');
            }
          } else {
            note('CONFLICT', abbr(day), employees[e].name + "'s choice #" + (rank + 1) +
              ' (' + SHIFTS[wanted] + ') is full');
          }
        }
      }

      // Anyone who asked for this day but is still unplaced: try another shift the same day.
      for (const e of order) {
        if (employees[e].prefs[day].length === 0) {
          continue;
        }
        if (assigned[e][day] !== -1) {
          continue;
        }
        if (daysWorked[e] >= MAX_DAYS) {
          note('DROPPED', abbr(day), employees[e].name + ' requested this day but already works ' +
            MAX_DAYS + ' days');
          continue;
        }
        const pick = bestShiftFor(employees[e].prefs[day], day);
        if (pick !== -1) {
          assign(e, day, pick);
          note('REASSIGNED', abbr(day), employees[e].name + ' moved to ' + SHIFTS[pick] +
            ' (all ranked shifts full)');
        } else {
          unplaced.push([e, day]);
        }
      }
    }

    // PHASE 1b - still unplaced: try the next day (after everyone's own requests were served).
    for (const [e, day] of unplaced) {
      const next = day + 1;
      let placed = false;
      if (next < DAYS.length && canWork(e, next)) {
        const pick = bestShiftFor(employees[e].prefs[day], next);
        if (pick !== -1) {
          assign(e, next, pick);
          note('CARRIED', abbr(day) + ' -> ' + abbr(next), employees[e].name + ' placed on ' +
            abbr(next) + ' ' + SHIFTS[pick]);
          placed = true;
        }
      }
      if (!placed) {
        note('UNPLACED', abbr(day), employees[e].name + ' could not be placed on ' + abbr(day) +
          ' or the next day');
      }
    }

    // PHASE 2 - top up any shift below the minimum with random eligible employees.
    for (let day = 0; day < DAYS.length; day++) {
      for (let shift = 0; shift < SHIFTS.length; shift++) {
        while (slots[day][shift].length < MIN_PER_SHIFT) {
          const pool = [];
          for (let e = 0; e < n; e++) {
            if (canWork(e, day)) {
              pool.push(e);
            }
          }
          if (pool.length === 0) {
            note('WARNING', abbr(day), SHIFTS[shift] + ' is short-staffed and nobody is eligible to add');
            break;
          }
          const chosen = pool[Math.floor(rand() * pool.length)];
          assign(chosen, day, shift);
          note('RANDOM', abbr(day), employees[chosen].name + ' added to ' + SHIFTS[shift] +
            ' (below minimum of ' + MIN_PER_SHIFT + ')');
        }
      }
    }

    return { employees, slots, assigned, daysWorked, log, seed, maxPerShift };
  }

  // ------------------------------------------------------------------ output

  /** Names working a shift, ordered by the employee list. */
  function namesAt(result, day, shift) {
    return result.slots[day][shift]
      .slice()
      .sort((a, b) => a - b)
      .map((e) => result.employees[e].name);
  }

  /** Checks computed from the finished schedule, independent of the scheduling code. */
  function runChecks(result) {
    const { employees, slots, maxPerShift } = result;
    const n = employees.length;
    const abbr = (day) => DAYS[day].slice(0, 3);

    const oneShift = [];
    for (let d = 0; d < DAYS.length; d++) {
      const seen = new Array(n).fill(0);
      for (let s = 0; s < SHIFTS.length; s++) {
        for (const e of slots[d][s]) {
          seen[e] += 1;
        }
      }
      for (let e = 0; e < n; e++) {
        if (seen[e] > 1) {
          oneShift.push(employees[e].name + ' on ' + abbr(d) + ' works ' + seen[e] + ' shifts');
        }
      }
    }

    const tooManyDays = [];
    for (let e = 0; e < n; e++) {
      let days = 0;
      for (let d = 0; d < DAYS.length; d++) {
        let works = false;
        for (let s = 0; s < SHIFTS.length; s++) {
          if (slots[d][s].includes(e)) {
            works = true;
          }
        }
        if (works) {
          days += 1;
        }
      }
      if (days > MAX_DAYS) {
        tooManyDays.push(employees[e].name + ' works ' + days + ' days');
      }
    }

    const understaffed = [];
    const overfull = [];
    for (let d = 0; d < DAYS.length; d++) {
      for (let s = 0; s < SHIFTS.length; s++) {
        const size = slots[d][s].length;
        if (size < MIN_PER_SHIFT) {
          understaffed.push(abbr(d) + ' ' + SHIFTS[s] + ' has ' + size + ' (minimum ' + MIN_PER_SHIFT + ')');
        }
        if (size > maxPerShift) {
          overfull.push(abbr(d) + ' ' + SHIFTS[s] + ' has ' + size + ' (max ' + maxPerShift + ')');
        }
      }
    }

    return [
      { level: oneShift.length ? 'FAIL' : 'OK', text: 'No employee works more than one shift per day', details: oneShift },
      { level: tooManyDays.length ? 'FAIL' : 'OK', text: 'No employee works more than ' + MAX_DAYS + ' days', details: tooManyDays },
      { level: understaffed.length ? 'WARN' : 'OK', text: 'Every shift has at least ' + MIN_PER_SHIFT + ' employees', details: understaffed },
      { level: overfull.length ? 'FAIL' : 'OK', text: 'No shift exceeds ' + maxPerShift + ' employees', details: overfull },
    ];
  }

  /** How well ranked preferences were honoured (bonus). */
  function preferenceSummary(result) {
    const { employees, assigned } = result;
    const summary = { requested: 0, choice: [0, 0, 0], differentShift: 0, notScheduled: 0, unrequested: 0 };
    for (let e = 0; e < employees.length; e++) {
      for (let d = 0; d < DAYS.length; d++) {
        const prefs = employees[e].prefs[d];
        if (prefs.length === 0) {
          if (assigned[e][d] !== -1) {
            summary.unrequested += 1;
          }
          continue;
        }
        summary.requested += 1;
        if (assigned[e][d] === -1) {
          summary.notScheduled += 1;
        } else {
          const rank = prefs.indexOf(assigned[e][d]);
          if (rank >= 0) {
            summary.choice[rank] += 1;
          } else {
            summary.differentShift += 1;
          }
        }
      }
    }
    return summary;
  }

  /** Plain-text report, identical to the Java console output. */
  function formatReport(result) {
    const { employees, assigned, daysWorked, log, seed, maxPerShift } = result;
    const banner = (title) => '==================== ' + title + ' ====================';
    const lines = [];
    let width = 4;
    for (const e of employees) {
      width = Math.max(width, e.name.length);
    }

    lines.push('Random seed: ' + seed + '   Max per shift: ' + maxPerShift);
    lines.push('');

    lines.push(banner('WEEKLY SCHEDULE'));
    for (let d = 0; d < DAYS.length; d++) {
      lines.push(DAYS[d]);
      for (let s = 0; s < SHIFTS.length; s++) {
        const names = namesAt(result, d, s);
        lines.push('  ' + SHIFTS[s].padEnd(9) + ' (' + names.length + '): ' + names.join(', '));
      }
      lines.push('');
    }

    lines.push(banner('EMPLOYEE VIEW'));
    lines.push('Name'.padEnd(width) + '  ' + DAYS.map((d) => d.slice(0, 3).padEnd(5)).join('') + 'Days');
    for (let e = 0; e < employees.length; e++) {
      const cells = DAYS.map((_, d) => (assigned[e][d] === -1 ? '-' : SHIFT_LETTERS[assigned[e][d]]).padEnd(5));
      lines.push(employees[e].name.padEnd(width) + '  ' + cells.join('') + daysWorked[e]);
    }
    lines.push('M = Morning, A = Afternoon, E = Evening, - = off');
    lines.push('');

    const sum = preferenceSummary(result);
    lines.push(banner('PREFERENCE SUMMARY'));
    const row = (label, value) => '  ' + label.padEnd(30) + ' ' + value;
    lines.push(row('Days requested', sum.requested));
    lines.push(row('Got 1st choice', sum.choice[0]));
    lines.push(row('Got 2nd choice', sum.choice[1]));
    lines.push(row('Got 3rd choice', sum.choice[2]));
    lines.push(row('Different shift', sum.differentShift));
    lines.push(row('Requested but not scheduled', sum.notScheduled));
    lines.push(row('Added on days not requested', sum.unrequested));
    lines.push('');

    lines.push(banner('SCHEDULING LOG'));
    if (log.length === 0) {
      lines.push('  (no conflicts or special assignments)');
    }
    for (const line of log) {
      lines.push('  ' + line);
    }
    lines.push('');

    lines.push(banner('CHECKS'));
    for (const check of runChecks(result)) {
      lines.push('  ' + ('[' + check.level + ']').padEnd(6) + ' ' + check.text);
      for (const detail of check.details) {
        lines.push('         - ' + detail);
      }
    }
    return lines.join('\n') + '\n';
  }

  return {
    DAYS, SHIFTS, SHIFT_LETTERS, MAX_DAYS, MIN_PER_SHIFT, DEFAULT_MAX_PER_SHIFT,
    parseShift, parseRanking, mulberry32, sampleEmployees,
    buildSchedule, namesAt, runChecks, preferenceSummary, formatReport,
  };
});
