import urllib.request
import json
import os
import uuid

TOKEN = "8913866950:AAFbYBWavHF8K8a0PcecuOeswffGC6J4-mk"
CHAT_ID = "7426550032"
APK_PATH = "docs/kapterka-pro-v3.4.4-b22.apk"
FILE_NAME = "kapterka-pro-v3.4.4-b22.apk"

size_mb = os.path.getsize(APK_PATH) / (1024 * 1024)
print(f"Uploading {FILE_NAME} ({size_mb:.2f} MB)...")

boundary = f"----WebKitFormBoundary{uuid.uuid4().hex}"
data = bytearray()

# chat_id
data.extend(f"--{boundary}\r\n".encode('utf-8'))
data.extend(b'Content-Disposition: form-data; name="chat_id"\r\n\r\n')
data.extend(f"{CHAT_ID}\r\n".encode('utf-8'))

# caption
caption = "⚡ Официальный чистый релиз: Каптёрка Про v3.4.4 (Сборка 22)\n📦 Размер: 17.9 МБ (оптимизированная релизная сборка без отладочных библиотек)"
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
    with urllib.request.urlopen(req, timeout=90) as resp:
        res = resp.read().decode('utf-8')
        print("Upload response:", res)
except Exception as e:
    print("Upload error:", e)
