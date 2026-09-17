with open("docs/index.html", "r", encoding="utf-8") as f:
    html = f.read()

old_banner = """              <div class="update-banner-icon">✨</div>
              <div class="update-banner-text">
                <strong>ОБНОВЛЕНИЕ 3.4.3 (СБОРКА 21): 100% СОВПАДЕНИЕ СКЛАДОВ И ОСТАТКОВ, БЕЗОПАСНАЯ СИНХРОНИЗАЦИЯ</strong>
                <p>Полное совпадение складов и остатков между всеми устройствами роты в реальном времени, моментальные уведомления и стабильная работа без сбоев.</p>
              </div>"""

new_banner = """              <div class="update-banner-icon">⚡</div>
              <div class="update-banner-text">
                <strong>ОБНОВЛЕНИЕ 3.4.4 (СБОРКА 22): ИСПРАВЛЕНО УДАЛЕНИЕ ТОЧЕК И ОСТАТКОВ В ОБЛАКЕ</strong>
                <p>При удалении точки учёта она мгновенно и безвозвратно удаляется у всех подключённых телефонов вместе со всеми остатками. Исправлена кнопка синхронизации — удалённые точки больше никогда не появляются заново.</p>
              </div>"""

if old_banner in html:
    html = html.replace(old_banner, new_banner, 1)
    with open("docs/index.html", "w", encoding="utf-8") as f:
        f.write(html)
    print("Banner updated successfully!")
else:
    print("Old banner not found, searching with regex...")
    import re
    html = re.sub(
        r'<div class="update-banner-icon">.*?</div>\s*<div class="update-banner-text">\s*<strong>.*?</strong>\s*<p>.*?</p>\s*</div>',
        new_banner,
        html,
        flags=re.DOTALL
    )
    with open("docs/index.html", "w", encoding="utf-8") as f:
        f.write(html)
    print("Banner replaced via regex!")

