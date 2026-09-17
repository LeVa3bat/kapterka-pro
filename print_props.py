import zipfile

for name in ["docs/kapterka-pro-v3.4.3-b21.apk", "docs/kapterka-pro-v3.4.4-b22.apk"]:
    z = zipfile.ZipFile(name)
    print("===", name, "===")
    for p in ["build-data.properties", "META-INF/com/android/build/gradle/app-metadata.properties"]:
        if p in z.namelist():
            print(f"[{p}]:\n{z.read(p).decode('utf-8', errors='ignore')}")
