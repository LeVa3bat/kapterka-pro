(function(){
  'use strict';
  window.dataLayer = window.dataLayer || [];
  window.gtag = window.gtag || function(){ window.dataLayer.push(arguments); };
  window.ym = window.ym || function(){ (window.ym.a = window.ym.a || []).push(arguments); };
  window.ym.l = window.ym.l || Date.now();

  gtag('js', new Date());
  gtag('config', 'G-RYV6TP63D3', { send_page_view: true });
  ym(112255061, 'init', {
    clickmap: !document.getElementById('reconcile-form'),
    referrer: document.referrer,
    url: location.href,
    accurateTrackBounce: true,
    trackLinks: !document.getElementById('reconcile-form'),
    webvisor: false
  });

  const start = (function scheduleAnalyticsLoad(){
    let started = false;
    let delayTimer;
    const interactionTypes = ['pointerdown','touchstart','keydown','scroll'];
    function whenIdle(task) {
      if (window.requestIdleCallback) window.requestIdleCallback(task, { timeout:2000 });
      else window.setTimeout(task, 0);
    }
    function loadScript(src, id, onDone) {
      if (document.getElementById(id)) { if (onDone) onDone(); return; }
      const script = document.createElement('script');
      script.id = id;
      script.async = true;
      script.fetchPriority = 'low';
      script.src = src;
      if (onDone) script.onload = script.onerror = onDone;
      document.head.appendChild(script);
    }
    function start() {
      if (started) return;
      started = true;
      window.clearTimeout(delayTimer);
      interactionTypes.forEach(type =>
        window.removeEventListener(type, start, { capture:true })
      );
      whenIdle(function() {
        let googleScheduled = false;
        function scheduleGoogle() {
          if (googleScheduled) return;
          googleScheduled = true;
          window.setTimeout(function() {
            whenIdle(function() {
              loadScript('https://www.googletagmanager.com/gtag/js?id=G-RYV6TP63D3', 'kapterka-guide-ga');
            });
          }, 1500);
        }
        // Сбой одного поставщика не должен останавливать второй счётчик.
        window.setTimeout(scheduleGoogle, 4000);
        loadScript('https://mc.yandex.ru/metrika/tag.js', 'kapterka-guide-ym', scheduleGoogle);
      });
    }
    interactionTypes.forEach(type =>
      window.addEventListener(type, start, { once:true, passive:true, capture:true })
    );
    // Очереди сохраняют ранние события; очень короткий визит до загрузки SDK может не учитываться.
    function startSoon() { if (!started) delayTimer = window.setTimeout(start, 5000); }
    if (document.readyState === 'complete') startSoon();
    else window.addEventListener('load', startSoon, { once:true });
    return start;
  })();

  function destination(link) {
    const href = link.getAttribute('href') || '';
    if (/kapterka-pro\.apk/i.test(href)) return 'apk';
    if (/(apps\.)?rustore\.ru/i.test(href)) return 'rustore';
    if (/^https:\/\/t\.me\/kapterka_pro(?:[/?#]|$)/i.test(href)) return 'telegram_channel';
    if (/#tabDownload/.test(href)) return 'download_section';
    if (/guides\.html/.test(href)) return 'guides';
    return 'internal';
  }
  document.addEventListener('click', function(e) {
    const link = e.target.closest('a');
    if (!link) return;
    const href = link.getAttribute('href') || '';
    if (/^downloads\/(workshop-(blank|example)\.(xlsx|pdf)|inventory-checklist\.pdf)$/.test(href)) window.trackPublicTool('template_download');
    if (/kapterka-pro\.apk/i.test(href)) window.trackPublicTool('apk_download');
    if (/(apps\.)?rustore\.ru/i.test(href)) window.trackPublicTool('rustore_open');
    const dest = destination(link);
    if (dest === 'telegram_channel') {
      try { gtag('event', 'telegram_channel_open', { source: 'site', page_path: location.pathname }); } catch(_) {}
      try { ym(112255061, 'reachGoal', 'telegram_channel_open', { source: 'site', page_path: location.pathname }); } catch(_) {}
      start();
    }
    if (!link.closest('.cta,.related,.release-list,.back,.page-links')) return;
    const params = {
      destination: dest,
      page_path: location.pathname,
      link_text: (link.textContent || '').trim().slice(0, 80)
    };
    try { gtag('event', 'guide_action', params); } catch(_) {}
    try { ym(112255061, 'reachGoal', 'guide_action', { destination: dest, page_path: location.pathname }); } catch(_) {}
    start();
  }, { capture:true });
  // В событиях инструментов только имя события и фиксированный путь страницы.
  // Ни названий, ни единиц, ни количеств, ни содержимого CSV.
  window.trackPublicTool = function(name) {
    if (!['template_download','calculator_use','calculator_export','rustore_open','apk_download','share_menu_requested'].includes(name)) return;
    try { gtag('event', name, {page_path: location.pathname}); } catch (_) {}
    try { ym(112255061, 'reachGoal', name, {page_path: location.pathname}); } catch (_) {}
    start();
  };
})();
