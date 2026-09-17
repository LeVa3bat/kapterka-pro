import urllib.request
import json

TOKEN = "8913866950:AAFbYBWavHF8K8a0PcecuOeswffGC6J4-mk"
CHAT_ID = "7426550032"

APK_URL = "https://ais-pre-6uqx367xvl7e3xkdwnduqu-787629332137.europe-west2.run.app/kapterka-pro-v3.4.5-b23.apk"
caption = """⚡ <b>Официальный релиз: Каптёрка Про v3.4.5 (Сборка 23)</b>
📦 <b>Размер: 17.9 МБ (чистая оптимизированная сборка)</b>

🔄 <b>Полное исправление синхронизации остатков между телефонами:</b>
• Приход, списание и перемещение отображаются на всех смартфонах в реальном времени.
• Позиции автоматически регистрируются в каталоге — карточки складов сразу показывают корректные остатки."""

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
    with urllib.request.urlopen(req, timeout=40) as resp:
        print("Telegram sendDocument response:", resp.read().decode('utf-8'))
except Exception as e:
    print("Error sending document:", e)
