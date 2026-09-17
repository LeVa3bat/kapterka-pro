import urllib.request
import json

TOKEN = "8913866950:AAFbYBWavHF8K8a0PcecuOeswffGC6J4-mk"
CHAT_ID = "7426550032"

text = """🚀 <b>ОБНОВЛЕНИЕ «КАПТЁРКА ПРО» v3.4.4 (Сборка 22)</b>

<b>Исправление синхронизации и удаления точек/складов:</b>

1. <b>Мгновенное удаление точек</b>: при удалении передовой точки или склада на одном телефоне она моментально удаляется у всех бойцов роты в реальном времени.
2. <b>Очистка остатков</b>: остатки удалённой точки безвозвратно удаляются как из локальной базы Room, так и из облака Firestore.
3. <b>Исправлена кнопка синхронизации</b>: удалённые точки больше никогда не «воскресают» и не появляются заново при ручном обновлении базы.
4. <b>Слушатель изменений</b>: моментальная зачистка данных на экранах у всех подключённых телефонов.

📲 <b>Прямая ссылка на скачивание APK v3.4.4:</b>
https://ais-pre-6uqx367xvl7e3xkdwnduqu-787629332137.europe-west2.run.app/kapterka-pro.apk

<i>(Также отправляется файл APK напрямую в чат)</i>"""

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

try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        print("Telegram notification sent successfully:", resp.read().decode('utf-8'))
except Exception as e:
    print("Error sending message:", e)
