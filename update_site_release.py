import shutil
import os

apk_src = "app/build/outputs/apk/debug/app-debug.apk"
if not os.path.exists(apk_src):
    print("Error: apk not found at", apk_src)
    exit(1)

# Destination paths in docs
dest_versioned = "docs/kapterka-pro-v3.4.4-b22.apk"
dest_main = "docs/kapterka-pro.apk"
dest_release = "docs/kapterka-release.apk"
dest_kapterka = "docs/kapterka.apk"

shutil.copy2(apk_src, dest_versioned)
shutil.copy2(apk_src, dest_main)
shutil.copy2(apk_src, dest_release)
shutil.copy2(apk_src, dest_kapterka)
shutil.copy2(apk_src, "Kapterka-debug.apk")

print(f"Copied APK to {dest_versioned} and main distribution files.")
print("APK file size:", os.path.getsize(dest_versioned), "bytes")
