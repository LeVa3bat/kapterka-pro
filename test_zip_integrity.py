import zipfile

for path in ["docs/kapterka-pro.apk", "docs/kapterka-pro-v3.4.4-b22.apk"]:
    with zipfile.ZipFile(path, "r") as z:
        test_result = z.testzip()
        print(f"{path} testzip:", "OK" if test_result is None else f"Corrupted: {test_result}")
        dex_files = [f for f in z.namelist() if f.endswith(".dex")]
        print(f"{path} dex files: {len(dex_files)}")
