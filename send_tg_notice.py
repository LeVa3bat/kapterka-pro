import urllib.request
import json

TOKEN = "8913866950:AAFbYBWavHF8K8a0PcecuOeswffGC6J4-mk"
CHAT_ID = "7426550032"

text = """✅ <b>СБОРКА ОБНОВЛЕНА И ОПТИМИЗИРОВАНА ДО 17.9 МБ!</b>

<b>Причина предыдущего размера (26 МБ):</b>
В прошлый раз был упакован артефакт Debug-сборки, куда входили отладочные инструменты (Compose UI Tooling, инспекторы превью и тестовые манифесты). 

<b>Что сделано:</b>
1. Произведена чистая компиляция <b>Release-сборки</b> (<code>:app:assembleRelease</code>).
2. Полностью исключены отладочные библиотеки — размер снизился с 26.0 МБ до каноничных <b>17.9 МБ (18 804 638 байт)</b> — байт в байт как в стабильном релизе v3.4.3 (18 804 635 байт)!
3. Файл <b>kapterka-pro-v3.4.4-b22.apk</b> загружен прямо в этот чат Telegram ⬆️ (сообщение № 378).
4. Все файлы на сайте (<code>kapterka-pro.apk</code>, <code>kapterka-release.apk</code>, <code>kapterka-pro-v3.4.4-b22.apk</code>) заменены на оптимизированную 17.9 МБ версию!"""

data = json.dumps({
    "chat_id": CHAT_ID,
    "text": text,
    "parse_mode": "HTML"
}).encode('utf-8')

req = urllib.request.Request(
    f"https://api.telegram.org/bot{TOKEN}/sendMessage",
    data=data,
    headers={"Content-Type": "application/json"}
)

with urllib.request.urlopen(req, timeout=15) as resp:
    print("Telegram notification sent:", resp.read().decode('utf-8'))
