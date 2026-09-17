import os
import requests

TOKEN = "8913866950:AAFbYBWavHF8K8a0PcecuOeswffGC6J4-mk"
CHAT_ID = "7426550032"
APK_PATH = "docs/kapterka-pro-v3.4.4-b22.apk"

caption = """🚀 <b>«Каптёрка Про» v3.4.4 (Сборка 22)</b>

<b>Полное обновление синхронизации и точек учёта:</b>

✅ <b>Синхронное удаление точек:</b> удаление точки на одном устройстве моментально удаляет её у всех бойцов подразделения.
✅ <b>Удаление остатков:</b> остатки удалённой точки полностью и безвозвратно вычищаются как локально, так и в облачной базе данных Firestore.
✅ <b>Исправлена кнопка синхронизации:</b> удалённые точки больше никогда не появляются заново при ручной синхронизации.
✅ <b>Мгновенные обновления:</b> улучшена стабильность передачи событий перемещения и списания между всеми телефонами онлайн.

<i>Файл APK прикреплён к данному сообщению для прямой установки.</i>"""

url = f"https://api.telegram.org/bot{TOKEN}/sendDocument"

print("Uploading APK to Telegram chat", CHAT_ID, "...")
with open(APK_PATH, "rb") as f:
    files = {"document": ("kapterka-pro-v3.4.4-b22.apk", f, "application/vnd.android.package-archive")}
    data = {
        "chat_id": CHAT_ID,
        "caption": caption,
        "parse_mode": "HTML"
    }
    response = requests.post(url, data=data, files=files, timeout=120)
    print("Response status:", response.status_code)
    print("Response body:", response.text)

