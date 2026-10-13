import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import test from 'node:test';

const index = fs.readFileSync(new URL('../../docs/index.html', import.meta.url), 'utf8');
const inline = [...index.matchAll(/<script>([\s\S]*?)<\/script>/g)]
  .map(match => match[1]).find(source => source.includes('scheduleAnalyticsLoad'));
assert.ok(inline, 'homepage analytics bootstrap must exist');
const bootstraps = [
  ['homepage', inline],
  ['guide', fs.readFileSync(new URL('../../docs/guide-analytics.js', import.meta.url), 'utf8')]
];

function browser(code, { idle = true, readyState = 'loading', calculator = false } = {}) {
  let now = 0, timerId = 0;
  const timers = new Map(), idleTasks = [], listeners = new Map(), scripts = [];
  const setTimeout = (fn, delay = 0) => {
    const id = ++timerId;
    timers.set(id, { fn, due: now + delay });
    return id;
  };
  const document = {
    readyState,
    referrer: 'https://kapterka-pro.ru/',
    getElementById: id => id === 'reconcile-form' && calculator
      ? {} : scripts.find(script => script.id === id),
    createElement: () => ({}),
    head: { appendChild: script => scripts.push(script) },
    addEventListener() {}
  };
  const context = {
    document, location: { href: 'https://kapterka-pro.ru/', pathname: '/' },
    setTimeout, clearTimeout: id => timers.delete(id),
    addEventListener(type, fn, options) {
      const callbacks = listeners.get(type) || [];
      callbacks.push({ fn, once: options?.once });
      listeners.set(type, callbacks);
    },
    removeEventListener(type, fn) {
      listeners.set(type, (listeners.get(type) || []).filter(item => item.fn !== fn));
    }
  };
  context.window = context;
  if (idle) context.requestIdleCallback = fn => idleTasks.push(fn);
  vm.runInNewContext(code, context);
  return {
    context, scripts, idleTasks,
    dispatch(type) {
      for (const item of [...(listeners.get(type) || [])]) {
        if (item.once) context.removeEventListener(type, item.fn);
        item.fn();
      }
    },
    runIdle() { for (const fn of idleTasks.splice(0)) fn(); },
    advance(ms) {
      const end = now + ms;
      while (true) {
        const next = [...timers.entries()].filter(([, task]) => task.due <= end)
          .sort((a, b) => a[1].due - b[1].due)[0];
        if (!next) break;
        now = next[1].due;
        timers.delete(next[0]);
        next[1].fn();
      }
      now = end;
    }
  };
}

for (const [name, code] of bootstraps) {
  test(`${name}: page view is queued immediately; vendors wait for load and idle`, () => {
    const b = browser(code);
    assert.equal(b.scripts.length, 0);
    assert.equal(b.context.dataLayer.filter(args => args[0] === 'config')[0][1], 'G-RYV6TP63D3');
    assert.equal(b.context.ym.a[0][0], 112255061);
    assert.equal(b.context.ym.a[0][1], 'init');
    b.advance(10000);
    assert.equal(b.scripts.length, 0, 'no vendor may load before page load');
    b.dispatch('load');
    b.advance(4999);
    assert.equal(b.idleTasks.length, 0);
    b.advance(1);
    assert.equal(b.scripts.length, 0, 'timer schedules idle work rather than loading inline');
    b.runIdle();
    assert.equal(b.scripts.length, 1);
    assert.match(b.scripts[0].src, /mc\.yandex\.ru/);
    assert.equal(b.scripts[0].async, true);
    assert.equal(b.scripts[0].fetchPriority, 'low');
    b.scripts[0].onload();
    b.advance(1499);
    assert.equal(b.scripts.length, 1);
    b.advance(1);
    b.runIdle();
    assert.equal(b.scripts.length, 2);
    assert.match(b.scripts[1].src, /googletagmanager\.com/);
    b.dispatch('pointerdown');
    b.scripts[0].onerror();
    b.advance(10000);
    b.runIdle();
    assert.equal(b.scripts.length, 2, 'each vendor loads once despite repeated triggers');
  });

  test(`${name}: early interaction keeps click events and loads outside the handler`, () => {
    const b = browser(code);
    b.context.gtag('event', 'rustore_open', { page_path: '/' });
    b.context.ym(112255061, 'reachGoal', 'rustore_open', { page_path: '/' });
    b.dispatch('pointerdown');
    assert.equal(b.scripts.length, 0);
    assert.equal(b.idleTasks.length, 1);
    b.dispatch('touchstart');
    b.runIdle();
    assert.equal(b.scripts.length, 1);
    assert.equal(b.context.dataLayer.filter(args => args[0] === 'event').length, 1);
    assert.equal(b.context.ym.a.filter(args => args[1] === 'reachGoal').length, 1);
    // A stalled first provider must not keep the second counter from loading.
    b.advance(5500);
    b.runIdle();
    assert.equal(b.scripts.length, 2);
  });

  test(`${name}: browsers without idle API still start both existing counters`, () => {
    const b = browser(code, { idle: false, readyState: 'complete' });
    b.advance(5000);
    assert.equal(b.scripts.length, 1);
    b.scripts[0].onerror();
    b.advance(1500);
    assert.equal(b.scripts.length, 2);
  });
}

test('calculator analytics excludes user input and preserves the event queue', () => {
  const b = browser(bootstraps[1][1], { calculator: true });
  assert.equal(b.context.ym.a[0][2].clickmap, false);
  assert.equal(b.context.ym.a[0][2].trackLinks, false);
  assert.equal(b.context.ym.a[0][2].webvisor, false);
  b.context.trackPublicTool('calculator_use');
  const event = b.context.dataLayer.find(args => args[0] === 'event');
  assert.equal(event[1], 'calculator_use');
  assert.deepEqual(Object.keys(event[2]), ['page_path']);
  assert.equal(b.scripts.length, 0);
  b.runIdle();
  assert.equal(b.scripts.length, 1);
});
