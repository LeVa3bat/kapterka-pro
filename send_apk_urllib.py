import urllib.request
import os
import uuid
import json

TOKEN = "8913866950:AAFbYBWavHF8K8a0PcecuOeswffGC6J4-mk"
CHAT_ID = "7426550032"
APK_PATH = "docs/kapterka-pro-v3.4.5-b23.apk"
FILE_NAME = "kapterka-pro-v3.4.5-b23.apk"

caption = """⚡ <b>«Каптёрка Про» v3.4.5 (Сборка 23)</b>

<b>Полное обновление синхронизации остатков:</b>
✅ <b>Синхронизация остатков:</b> приход, перемещение и списание отображаются на всех устройствах роты в реальном времени.
✅ <b>Авторегистрация позиций:</b> новые наименования из облака мгновенно распознаются и выводятся в карточках складов и каталоге без скрытия.
✅ <b>Оптимизация размера:</b> чистый релиз 17.9 МБ без отладочных библиотек.
✅ <b>Удаление точек:</b> каскадная зачистка складов и остатков в Room и Firestore.

<i>Файл APK прикреплён к данному сообщению для прямой установки.</i>"""

boundary = uuid.uuid4().hex

def encode_multipart(fields, files):
    body = bytearray()
    for key, value in fields.items():
        body.extend(f'--{boundary}\r\n'.encode('utf-8'))
        body.extend(f'Content-Disposition: form-data; name="{key}"\r\n\r\n'.encode('utf-8'))
        body.extend(f'{value}\r\n'.encode('utf-8'))
    for key, (filename, file_bytes, content_type) in files.items():
        body.extend(f'--{boundary}\r\n'.encode('utf-8'))
        body.extend(f'Content-Disposition: form-data; name="{key}"; filename="{filename}"\r\n'.encode('utf-8'))
        body.extend(f'Content-Type: {content_type}\r\n\r\n'.encode('utf-8'))
        body.extend(file_bytes)
        body.extend(b'\r\n')
    body.extend(f'--{boundary}--\r\n'.encode('utf-8'))
    return body

print(f"Reading {APK_PATH}...")
with open(APK_PATH, "rb") as f:
    apk_data = f.read()

size_mb = len(apk_data) / (1024 * 1024)
print(f"APK size: {size_mb:.2f} MB ({len(apk_data)} bytes). Preparing payload...")

fields = {
    "chat_id": CHAT_ID,
    "caption": caption,
    "parse_mode": "HTML"
}
files = {
    "document": (FILE_NAME, apk_data, "application/vnd.android.package-archive")
}

body = encode_multipart(fields, files)
url = f"https://api.telegram.org/bot{TOKEN}/sendDocument"

req = urllib.request.Request(url, data=body)
req.add_header('Content-Type', f'multipart/form-data; boundary={boundary}')
req.add_header('Content-Length', str(len(body)))

print("Sending request to Telegram API (timeout=300s)...")
try:
    with urllib.request.urlopen(req, timeout=300) as response:
        res = response.read().decode('utf-8')
        print("Telegram API response:", res)
except Exception as e:
    print("Error sending to telegram:", e)
