import shutil
import os

src = "app/build/outputs/apk/release/app-release.apk"

targets = [
    "docs/kapterka-pro.apk",
    "docs/kapterka-release.apk",
    "docs/kapterka.apk",
    "docs/kapterka-pro-v3.4.4-b22.apk"
]

for t in targets:
    shutil.copy2(src, t)
    size = os.path.getsize(t)
    print(f"Updated {t}: {size} bytes ({size / (1024*1024):.2f} MB)")
