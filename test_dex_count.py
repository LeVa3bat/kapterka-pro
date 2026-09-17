import zipfile

for apk in ["docs/kapterka-pro-v3.4.3-b21.apk", "app/build/outputs/apk/debug/app-debug.apk"]:
    z = zipfile.ZipFile(apk)
    dex_files = [f for f in z.namelist() if f.endswith(".dex")]
    total_raw = sum(z.getinfo(f).file_size for f in dex_files)
    total_comp = sum(z.getinfo(f).compress_size for f in dex_files)
    print(apk)
    print(f"  dex count: {len(dex_files)}")
    print(f"  dex raw: {total_raw / (1024*1024):.2f} MB, comp: {total_comp / (1024*1024):.2f} MB")
    print(f"  total apk: {sum(z.getinfo(f).compress_size for f in z.namelist()) / (1024*1024):.2f} MB")
