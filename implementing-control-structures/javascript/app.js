/* Browser GUI: collects employee names and ranked shift preferences, then shows the schedule. */
(function () {
  'use strict';

  const { DAYS, SHIFTS, SHIFT_LETTERS, MIN_PER_SHIFT } = Scheduler;
  const $ = (selector) => document.querySelector(selector);

  /** employees: [{ name, prefs: [[shiftIndex, ...] x 7] }] */
  let employees = [];
  let lastResult = null;

  // ------------------------------------------------------------------ helpers

  function newEmployee(name) {
    return { name: name || '', prefs: DAYS.map(() => []) };
  }

  function showMessage(text) {
    const box = $('#message');
    box.textContent = text;
    box.classList.toggle('show', Boolean(text));
  }

  function el(tag, props, children) {
    const node = document.createElement(tag);
    Object.assign(node, props || {});
    for (const child of children || []) {
      node.append(child);
    }
    return node;
  }

  function hue(index) {
    return (index * 47) % 360;
  }

  // ------------------------------------------------------------ input screen

  function shiftSelect(value) {
    const select = el('select');
    select.append(el('option', { value: '', textContent: '–' }));
    SHIFTS.forEach((label, i) => select.append(el('option', { value: String(i), textContent: label })));
    select.value = value === undefined ? '' : String(value);
    return select;
  }

  /**
   * Keeps one day's three selects consistent: no repeats and no gaps
   * (a 2nd choice without a 1st choice slides up to become the 1st).
   */
  function normalizeDay(emp, day, selects) {
    const ranked = [];
    for (const select of selects) {
      if (select.value !== '' && !ranked.includes(Number(select.value))) {
        ranked.push(Number(select.value));
      }
    }
    selects.forEach((select, k) => {
      select.value = ranked[k] === undefined ? '' : String(ranked[k]);
    });
    emp.prefs[day] = ranked;
  }

  function employeeCard(emp, index) {
    const nameInput = el('input', { type: 'text', value: emp.name, placeholder: 'Employee name' });
    nameInput.setAttribute('aria-label', 'Employee name');
    nameInput.addEventListener('input', () => { emp.name = nameInput.value; });

    const removeBtn = el('button', { type: 'button', className: 'small danger', textContent: 'Remove' });
    removeBtn.addEventListener('click', () => {
      employees.splice(index, 1);
      renderEmployees();
    });

    const body = el('tbody');
    const rowSelects = [];
    DAYS.forEach((dayName, day) => {
      const selects = [0, 1, 2].map((k) => {
        const select = shiftSelect(emp.prefs[day][k]);
        select.setAttribute('aria-label', dayName + ' choice ' + (k + 1));
        return select;
      });
      selects.forEach((select) => {
        select.addEventListener('change', () => normalizeDay(emp, day, selects));
      });
      rowSelects.push(selects);
      body.append(el('tr', {}, [
        el('td', { className: 'day', textContent: dayName.slice(0, 3) }),
        ...selects.map((select) => el('td', {}, [select])),
      ]));
    });

    const copyBtn = el('button', { type: 'button', className: 'small', textContent: 'Copy Monday to all days' });
    copyBtn.addEventListener('click', () => {
      for (let day = 1; day < DAYS.length; day++) {
        emp.prefs[day] = emp.prefs[0].slice();
        rowSelects[day].forEach((select, k) => {
          select.value = emp.prefs[day][k] === undefined ? '' : String(emp.prefs[day][k]);
        });
      }
    });

    const table = el('table', {}, [
      el('thead', {}, [el('tr', {}, ['', '1st', '2nd', '3rd'].map((t) => el('th', { textContent: t })))]),
      body,
    ]);

    return el('div', { className: 'card' }, [
      el('div', { className: 'card-head' }, [nameInput, removeBtn]),
      table,
      el('div', { className: 'card-foot' }, [copyBtn]),
    ]);
  }

  function renderEmployees() {
    const host = $('#cards');
    host.replaceChildren();
    if (employees.length === 0) {
      host.append(el('div', { className: 'empty', textContent: 'No employees yet. Add one, or load the sample data.' }));
      return;
    }
    employees.forEach((emp, i) => host.append(employeeCard(emp, i)));
  }

  // ----------------------------------------------------------- output screen

  function chip(name, index) {
    const node = el('span', { className: 'chip', textContent: name });
    node.style.setProperty('--h', String(hue(index)));
    return node;
  }

  function renderSchedule(result) {
    const grid = $('#schedule-grid');
    grid.replaceChildren();

    grid.append(el('thead', {}, [el('tr', {}, [
      el('th', { textContent: '' }),
      ...DAYS.map((d) => el('th', { textContent: d })),
    ])]));

    const body = el('tbody');
    SHIFTS.forEach((shiftName, s) => {
      const row = el('tr', {}, [el('th', { className: 'shift', scope: 'row', textContent: shiftName })]);
      DAYS.forEach((_, d) => {
        const ids = result.slots[d][s].slice().sort((a, b) => a - b);
        const cell = el('td', { className: ids.length < MIN_PER_SHIFT ? 'low' : '' }, [
          el('span', { className: 'count', textContent: ids.length + (ids.length === 1 ? ' person' : ' people') }),
        ]);
        ids.forEach((id) => cell.append(chip(result.employees[id].name, id)));
        row.append(cell);
      });
      body.append(row);
    });
    grid.append(body);
  }

  function renderEmployeeView(result) {
    const grid = $('#employee-grid');
    grid.replaceChildren();
    grid.append(el('thead', {}, [el('tr', {}, [
      el('th', { textContent: 'Name' }),
      ...DAYS.map((d) => el('th', { textContent: d.slice(0, 3) })),
      el('th', { textContent: 'Days' }),
    ])]));

    const body = el('tbody');
    result.employees.forEach((emp, e) => {
      const row = el('tr', {}, [el('td', {}, [chip(emp.name, e)])]);
      DAYS.forEach((_, d) => {
        const shift = result.assigned[e][d];
        row.append(el('td', {
          className: shift === -1 ? 'off' : '',
          textContent: shift === -1 ? '–' : SHIFT_LETTERS[shift],
          title: shift === -1 ? 'Off' : SHIFTS[shift],
        }));
      });
      const days = result.daysWorked[e];
      row.append(el('td', { className: days >= Scheduler.MAX_DAYS ? 'full' : '', textContent: String(days) }));
      body.append(row);
    });
    grid.append(body);
  }

  function renderStats(result) {
    const sum = Scheduler.preferenceSummary(result);
    const items = [
      [sum.requested, 'days requested'],
      [sum.choice[0], 'got 1st choice'],
      [sum.choice[1], 'got 2nd choice'],
      [sum.choice[2], 'got 3rd choice'],
      [sum.differentShift, 'different shift'],
      [sum.notScheduled, 'not scheduled'],
      [sum.unrequested, 'added on other days'],
    ];
    const host = $('#stats');
    host.replaceChildren();
    for (const [value, label] of items) {
      host.append(el('div', { className: 'stat' }, [
        el('b', { textContent: String(value) }),
        el('span', { textContent: label }),
      ]));
    }
  }

  function renderChecks(result) {
    const host = $('#checks');
    host.replaceChildren();
    const icons = { OK: '✔', WARN: '▲', FAIL: '✖' };
    for (const check of Scheduler.runChecks(result)) {
      const item = el('li', {}, [
        el('span', {
          className: 'check-' + check.level.toLowerCase(),
          textContent: icons[check.level] + ' ',
        }),
        document.createTextNode(check.text),
      ]);
      if (check.details.length > 0) {
        item.append(el('div', { className: 'check-detail', textContent: check.details.join('; ') }));
      }
      host.append(item);
    }
  }

  function renderLog(result) {
    const host = $('#log');
    host.replaceChildren();
    if (result.log.length === 0) {
      host.textContent = '(no conflicts or special assignments)';
      return;
    }
    for (const line of result.log) {
      const tag = line.slice(0, 10).trim();
      host.append(el('div', {}, [
        el('span', { className: 'tag', textContent: tag.padEnd(11) }),
        document.createTextNode(line.slice(11)),
      ]));
    }
  }

  // ------------------------------------------------------------------ actions

  function validate() {
    if (employees.length === 0) {
      return 'Add at least one employee first.';
    }
    const seen = new Set();
    for (let i = 0; i < employees.length; i++) {
      const name = employees[i].name.trim();
      if (name === '') {
        return 'Employee ' + (i + 1) + ' needs a name.';
      }
      if (seen.has(name.toLowerCase())) {
        return 'The name "' + name + '" is used twice. Please make names unique.';
      }
      seen.add(name.toLowerCase());
    }
    const max = Number($('#max-input').value);
    if (!Number.isInteger(max) || max < MIN_PER_SHIFT) {
      return 'Max per shift must be a whole number of at least ' + MIN_PER_SHIFT + '.';
    }
    if (!Number.isInteger(Number($('#seed-input').value))) {
      return 'The random seed must be a whole number.';
    }
    return '';
  }

  function generate() {
    const problem = validate();
    showMessage(problem);
    if (problem) {
      return;
    }
    const people = employees.map((e) => ({ name: e.name.trim(), prefs: e.prefs.map((p) => p.slice()) }));
    lastResult = Scheduler.buildSchedule(people, {
      maxPerShift: Number($('#max-input').value),
      seed: Number($('#seed-input').value),
    });
    renderSchedule(lastResult);
    renderEmployeeView(lastResult);
    renderStats(lastResult);
    renderChecks(lastResult);
    renderLog(lastResult);
    $('#copy-status').textContent = '';
    $('#output-section').hidden = false;
    $('#output-section').scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  function init() {
    $('#add-btn').addEventListener('click', () => {
      employees.push(newEmployee(''));
      renderEmployees();
      const inputs = document.querySelectorAll('.card-head input');
      inputs[inputs.length - 1].focus();
    });
    $('#sample-btn').addEventListener('click', () => {
      employees = Scheduler.sampleEmployees();
      showMessage('');
      renderEmployees();
    });
    $('#clear-btn').addEventListener('click', () => {
      employees = [];
      lastResult = null;
      $('#output-section').hidden = true;
      showMessage('');
      renderEmployees();
    });
    $('#shuffle-btn').addEventListener('click', () => {
      $('#seed-input').value = String(Math.floor(Math.random() * 1000000));
    });
    $('#generate-btn').addEventListener('click', generate);
    $('#copy-btn').addEventListener('click', async () => {
      if (!lastResult) {
        return;
      }
      try {
        await navigator.clipboard.writeText(Scheduler.formatReport(lastResult));
        $('#copy-status').textContent = 'Copied.';
      } catch (err) {
        $('#copy-status').textContent = 'Copy is not available in this browser context.';
      }
    });
    renderEmployees();
  }

  init();
})();
