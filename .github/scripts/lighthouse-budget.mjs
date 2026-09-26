import fs from 'node:fs';
const files = process.argv.slice(2);
if (!files.length) throw new Error('Pass Lighthouse JSON report paths');
let failed = false;
// Яндекс.Метрика ставит сторонние cookie (sync_cookie на mc.yandex.*). Lighthouse снижает за них
// «Best practices» (third-party-cookies, inspector-issues), хотя это осознанный счётчик сайта.
// Эти две проверки не считаем, только если все нарушения идут с mc.yandex.*.
const isYandexOnly = audit => {
  const urls = [];
  for (const it of audit?.details?.items ?? []) {
    if (it.url) urls.push(it.url);
    for (const sub of it.subItems?.items ?? []) if (sub.url) urls.push(sub.url);
  }
  return urls.length > 0 && urls.every(u => /^https:\/\/mc\.yandex\./.test(u));
};
const bestPractices = report => {
  const skip = new Set(['third-party-cookies', 'inspector-issues'].filter(id => isYandexOnly(report.audits?.[id])));
  let sum = 0, weight = 0;
  for (const ref of report.categories?.['best-practices']?.auditRefs ?? []) {
    const a = report.audits?.[ref.id];
    if (!ref.weight || skip.has(ref.id) || a?.score == null) continue;
    sum += ref.weight * a.score; weight += ref.weight;
  }
  return weight ? Math.round(sum / weight * 100) : 0;
};
for (const file of files) {
  const report = JSON.parse(fs.readFileSync(file, 'utf8'));
  const category = name => Math.round(((report.categories?.[name]?.score ?? 0) * 100));
  const auditValue = id => report.audits?.[id]?.numericValue;
  const perf = category('performance');
  const a11y = category('accessibility');
  const best = bestPractices(report);
  const seo = category('seo');
  const lcp = auditValue('largest-contentful-paint');
  const cls = auditValue('cumulative-layout-shift');
  const tbt = auditValue('total-blocking-time');
  console.log(`\n${file}`);
  console.log(`Performance: ${perf}`);
  console.log(`Accessibility: ${a11y}`);
  console.log(`Best practices: ${best}`);
  console.log(`SEO: ${seo}`);
  if (Number.isFinite(lcp)) console.log(`LCP: ${Math.round(lcp)} ms`);
  if (Number.isFinite(cls)) console.log(`CLS: ${cls.toFixed(3)}`);
  if (Number.isFinite(tbt)) console.log(`TBT: ${Math.round(tbt)} ms`);
  if (a11y < 90) { console.error('FAIL: accessibility below 90'); failed = true; }
  if (best < 85) { console.error('FAIL: best-practices below 85'); failed = true; }
  if (seo < 90) { console.error('FAIL: SEO below 90'); failed = true; }
  if (perf < 65) console.warn('WARN: performance below 65');
  if (Number.isFinite(lcp) && lcp > 4000) console.warn('WARN: Lighthouse LCP above 4s');
  if (Number.isFinite(cls) && cls > 0.15) console.warn('WARN: Lighthouse CLS above 0.15');
}
if (failed) process.exit(1);
console.log('\nLighthouse quality gates PASSED.');
