(() => {
  'use strict';
  const setText = (selector, value) => {
    if (value === undefined || value === null) return;
    document.querySelectorAll(selector).forEach(el => { el.textContent = String(value); });
  };
  async function hydrateReleaseMeta() {
    try {
      const response = await fetch('release.json?_=' + Date.now(), { cache: 'no-store' });
      if (!response.ok) return;
      const release = await response.json();
      if (!release || !release.versionName || !release.versionCode) return;
      setText('[data-release-version]', release.versionName);
      setText('[data-release-code]', release.versionCode);
      setText('[data-release-android]', release.minAndroid);
      setText('[data-release-status]', release.statusLabel || 'Стабильный релиз');
      setText('[data-release-type]', release.releaseType || 'обновление');
      setText('[data-release-package]', release.packageName);
      setText('[data-release-sha]', release.apkSha256);
      setText('[data-release-signer]', release.signerSha256);
      document.querySelectorAll('[data-release-rustore]').forEach(link => {
        if (release.rustoreUrl) link.href = release.rustoreUrl;
      });
      const highlights = document.querySelector('[data-release-highlights]');
      if (highlights && Array.isArray(release.highlights) && release.highlights.length) {
        highlights.replaceChildren(...release.highlights.map(text => {
          const li = document.createElement('li');
          li.textContent = /[;.]$/.test(text) ? text : text + ';';
          return li;
        }));
      }
    } catch (_) {}
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', hydrateReleaseMeta, { once: true });
  else hydrateReleaseMeta();
})();
