#!/usr/bin/env node
/*
 * Command-line runner for the JavaScript scheduler (uses the built-in sample data).
 * The interactive input/output screens live in index.html + app.js.
 *
 *   node cli.js [--seed N] [--max N]
 */
const Scheduler = require('./scheduler.js');

let seed = 42;
let maxPerShift = Scheduler.DEFAULT_MAX_PER_SHIFT;

// Command-line options (while loop + switch).
const args = process.argv.slice(2);
let i = 0;
while (i < args.length) {
  switch (args[i]) {
    case '--seed':
      seed = parseInt(args[++i], 10);
      break;
    case '--max':
      maxPerShift = parseInt(args[++i], 10);
      break;
    case '--demo':
      break; // accepted so the command matches the Java version
    default:
      console.error('Usage: node cli.js [--seed N] [--max N]');
      process.exit(1);
  }
  i++;
}

if (Number.isNaN(seed) || Number.isNaN(maxPerShift) || maxPerShift < Scheduler.MIN_PER_SHIFT) {
  console.error('--seed needs a whole number and --max must be at least ' + Scheduler.MIN_PER_SHIFT + '.');
  process.exit(1);
}

const result = Scheduler.buildSchedule(Scheduler.sampleEmployees(), { seed, maxPerShift });
process.stdout.write('\n' + Scheduler.formatReport(result));
