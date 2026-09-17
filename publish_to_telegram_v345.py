import urllib.request
import json
import os
import uuid

TOKEN = "8913866950:AAFbYBWavHF8K8a0PcecuOeswffGC6J4-mk"
CHAT_ID = "7426550032"
APK_PATH = "docs/kapterka-pro-v3.4.5-b23.apk"
FILE_NAME = "kapterka-pro-v3.4.5-b23.apk"

size_mb = os.path.getsize(APK_PATH) / (1024 * 1024)
print(f"Uploading {FILE_NAME} ({size_mb:.2f} MB) to Telegram...")

boundary = f"----WebKitFormBoundary{uuid.uuid4().hex}"
data = bytearray()

# chat_id
data.extend(f"--{boundary}\r\n".encode('utf-8'))
data.extend(b'Content-Disposition: form-data; name="chat_id"\r\n\r\n')
data.extend(f"{CHAT_ID}\r\n".encode('utf-8'))

# caption
caption = "⚡ Официальный релиз: Каптёрка Про v3.4.5 (Сборка 23)\n📦 Размер: 17.9 МБ (чистый релиз)\n🔄 Полное исправление синхронизации остатков и данных между смартфонами"
data.extend(f"--{boundary}\r\n".encode('utf-8'))
data.extend(b'Content-Disposition: form-data; name="caption"\r\n\r\n')
data.extend(f"{caption}\r\n".encode('utf-8'))

# document file
data.extend(f"--{boundary}\r\n".encode('utf-8'))
data.extend(f'Content-Disposition: form-data; name="document"; filename="{FILE_NAME}"\r\n'.encode('utf-8'))
data.extend(b'Content-Type: application/vnd.android.package-archive\r\n\r\n')

with open(APK_PATH, "rb") as f:
    data.extend(f.read())

data.extend(f"\r\n--{boundary}--\r\n".encode('utf-8'))

req = urllib.request.Request(
    f"https://api.telegram.org/bot{TOKEN}/sendDocument",
    data=bytes(data),
    headers={
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "Content-Length": str(len(data))
    }
)

try:
    with urllib.request.urlopen(req, timeout=120) as resp:
        res = resp.read().decode('utf-8')
        print("Upload APK response:", res)
except Exception as e:
    print("Upload APK error:", e)

# 2. Send Full Release Announcement
release_post = """📢 <b>ОФИЦИАЛЬНАЯ ПУБЛИКАЦИЯ РЕЛИЗА НА САЙТЕ И В ТГ КАНАЛЕ</b>

⚡ <b>«КАПТЁРКА ПРО» v3.4.5 (СБОРКА 23)</b>
📅 <i>Дата релиза: 17 сентября 2026 г.</i>
📦 <i>Размер файла: 17.9 МБ (чистая оптимизированная сборка)</i>

━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎯 <b>КЛЮЧЕВЫЕ ИСПРАВЛЕНИЯ И ОБНОВЛЕНИЯ:</b>

1. 🔄 <b>Полная синхронизация остатков между устройствами в реальном времени:</b>
• Устранена проблема, когда на первом телефоне проводился приход, а на втором складе остатки не появлялись.
• Реализована автоматическая авторегистрация новых позиций: при поступлении имущества по облаку оно мгновенно попадает в локальный каталог второго устройства и сразу же отображается в карточках складов и таблицах остатков.

2. 📊 <b>Корректный подсчет и отображение в карточках складов:</b>
• Исправлен алгоритм агрегации остатков в списках складов — теперь учитываются абсолютно все позиции, пришедшие из облака, даже если они отсутствовали в базовом предзагруженном списке.
• Чипы складов, счетчики наименований и суммарные остатки показывают точные и одинаковые данные на всех подключенных устройствах.

3. 🚀 <b>Оптимизация размера сборки (17.9 МБ):</b>
• Сборка собрана в чистом релизном профиле Proguard/R8 без отладочных библиотек превью.
• Вес снижен до каноничных 17.9 МБ для максимально быстрой загрузки в полевых условиях при слабом сигнале сети.

4. 🗑️ <b>Синхронное каскадное удаление точек учёта:</b>
• При удалении передовой точки или склада на одном смартфоне она мгновенно исчезает у всех бойцов роты в режиме реального времени.
• Остатки удаленной точки каскадно зачищаются в локальной базе Room и в облачной базе Firestore — больше никаких «зависших» остатков.

5. 🛡️ <b>Надежность оффлайн-сейфа:</b>
• Защита от потери ключа подразделения и стабильная работа при сбоях РЭБ.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📲 <b>СКАЧАТЬ ОБНОВЛЕНИЕ:</b>
• Файл APK загружен документом прямо в этот чат ⬆️
• Прямая ссылка для скачивания на сайте:
https://ais-pre-6uqx367xvl7e3xkdwnduqu-787629332137.europe-west2.run.app/kapterka-pro.apk
• Главная страница сайта:
https://ais-pre-6uqx367xvl7e3xkdwnduqu-787629332137.europe-west2.run.app/"""

post_data = json.dumps({
    "chat_id": CHAT_ID,
    "text": release_post,
    "parse_mode": "HTML",
    "disable_web_page_preview": False
}).encode('utf-8')

req_post = urllib.request.Request(
    f"https://api.telegram.org/bot{TOKEN}/sendMessage",
    data=post_data,
    headers={"Content-Type": "application/json"}
)

try:
    with urllib.request.urlopen(req_post, timeout=30) as resp:
        print("Release post sent:", resp.read().decode('utf-8'))
except Exception as e:
    print("Release post error:", e)
