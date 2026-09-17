import urllib.request
import json

TOKEN = "8913866950:AAFbYBWavHF8K8a0PcecuOeswffGC6J4-mk"
CHAT_ID = "7426550032"
# Public direct URL of the compiled APK on our deployed web server
APK_URL = "https://ais-pre-6uqx367xvl7e3xkdwnduqu-787629332137.europe-west2.run.app/kapterka-pro-v3.4.4-b22.apk"

caption = """📦 <b>Каптёрка Про v3.4.4 (Сборка 22)</b>

<b>Установочный файл APK с исправлением точек и остатков.</b>

✅ Синхронное удаление точек на всех телефонах
✅ Полное удаление остатков в облаке Firestore и локально
✅ Исправлена кнопка синхронизации (точки не восстанавливаются)"""

data = json.dumps({
    "chat_id": CHAT_ID,
    "document": APK_URL,
    "caption": caption,
    "parse_mode": "HTML"
}).encode('utf-8')

req = urllib.request.Request(
    f"https://api.telegram.org/bot{TOKEN}/sendDocument",
    data=data,
    headers={"Content-Type": "application/json"}
)

try:
    with urllib.request.urlopen(req, timeout=30) as resp:
        print("Telegram sendDocument by URL response:", resp.read().decode('utf-8'))
except Exception as e:
    print("Error sending document by URL:", e)
