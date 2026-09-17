with open("docs/kapterka-pro-v3.4.3-b21.apk", "rb") as f:
    data = f.read()

import zipfile
import io

z = zipfile.ZipFile(io.BytesIO(data))
print("Files in b21:")
for n in z.namelist():
    if "classes" in n or "build" in n:
        print(" ", n, z.getinfo(n).file_size, z.getinfo(n).compress_size)
