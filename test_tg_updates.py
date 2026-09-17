import urllib.request
import json

TOKEN = "8913866950:AAFbYBWavHF8K8a0PcecuOeswffGC6J4-mk"
CHAT_ID = "7426550032"

# Let's send a verification message with message_id return
req = urllib.request.Request(
    f"https://api.telegram.org/bot{TOKEN}/sendMessage",
    data=json.dumps({"chat_id": CHAT_ID, "text": "🔔 Проверка связи канала релиза."}).encode('utf-8'),
    headers={"Content-Type": "application/json"}
)
with urllib.request.urlopen(req, timeout=10) as resp:
    res = json.loads(resp.read().decode('utf-8'))
    print("Latest message_id in chat:", res["result"]["message_id"])
