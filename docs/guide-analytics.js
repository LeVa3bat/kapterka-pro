(function(){
  'use strict';
  window.dataLayer = window.dataLayer || [];
  window.gtag = window.gtag || function(){ window.dataLayer.push(arguments); };
  window.ym = window.ym || function(){ (window.ym.a = window.ym.a || []).push(arguments); };
  window.ym.l = window.ym.l || Date.now();

  gtag('js', new Date());
  gtag('config', 'G-RYV6TP63D3', { send_page_view: true });
  ym(112255061, 'init', {
    clickmap: true,
    referrer: document.referrer,
    url: location.href,
    accurateTrackBounce: true,
    trackLinks: true,
    webvisor: false
  });

  let started = false;
  function loadScript(src, id) {
    if (document.getElementById(id)) return;
    const s = document.createElement('script');
    s.id = id;
    s.async = true;
    s.src = src;
    document.head.appendChild(s);
  }
  function start() {
    if (started) return;
    started = true;
    loadScript('https://www.googletagmanager.com/gtag/js?id=G-RYV6TP63D3', 'kapterka-guide-ga');
    loadScript('https://mc.yandex.ru/metrika/tag.js', 'kapterka-guide-ym');
  }
  ['pointerdown','touchstart','keydown','scroll'].forEach(type =>
    window.addEventListener(type, start, { once:true, passive:true, capture:true })
  );
  // Считаем и короткие визиты: грузим счётчики вскоре после события load, а не по таймеру в несколько секунд.
  function startSoon() { setTimeout(start, 1200); }
  if (document.readyState === 'complete') startSoon();
  else window.addEventListener('load', startSoon, { once: true });

  function destination(link) {
    const href = link.getAttribute('href') || '';
    if (/kapterka-pro\.apk/i.test(href)) return 'apk';
    if (/apps\.rustore\.ru/i.test(href)) return 'rustore';
    if (/#tabDownload/.test(href)) return 'download_section';
    if (/guides\.html/.test(href)) return 'guides';
    return 'internal';
  }
  document.addEventListener('click', function(e) {
    const link = e.target.closest('a');
    if (!link) return;
    if (!link.closest('.cta,.related,.release-list,.back,.page-links')) return;
    const dest = destination(link);
    const params = {
      destination: dest,
      page_path: location.pathname,
      link_text: (link.textContent || '').trim().slice(0, 80)
    };
    try { gtag('event', 'guide_action', params); } catch(_) {}
    try { ym(112255061, 'reachGoal', 'guide_action', { destination: dest, page_path: location.pathname }); } catch(_) {}
    start();
  }, { capture:true });
})();
