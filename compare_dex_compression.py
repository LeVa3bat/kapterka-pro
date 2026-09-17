import zipfile

z21 = zipfile.ZipFile("docs/kapterka-pro-v3.4.3-b21.apk")
z22 = zipfile.ZipFile("docs/kapterka-pro-v3.4.4-b22.apk")

print("b21 total compressed:", sum(i.compress_size for i in z21.infolist()))
print("b22 total compressed:", sum(i.compress_size for i in z22.infolist()))

for i21 in z21.infolist():
    if "classes" in i21.filename:
        print(f"b21 {i21.filename}: raw={i21.file_size} comp={i21.compress_size} method={i21.compress_type}")

print("---")
for i22 in z22.infolist():
    if "classes" in i22.filename:
        print(f"b22 {i22.filename}: raw={i22.file_size} comp={i22.compress_size} method={i22.compress_type}")
