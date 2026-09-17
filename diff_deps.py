import zipfile

z21 = zipfile.ZipFile("docs/kapterka-pro-v3.4.3-b21.apk")
z22 = zipfile.ZipFile("docs/kapterka-pro-v3.4.4-b22.apk")

# List all META-INF *.version or pom or libraries
print("--- META-INF in b21 ---")
for n in sorted(z21.namelist()):
    if n.startswith("META-INF/") and (n.endswith(".version") or n.endswith(".properties")):
        print(" ", n)

print("--- META-INF in b22 ---")
for n in sorted(z22.namelist()):
    if n.startswith("META-INF/") and (n.endswith(".version") or n.endswith(".properties")):
        print(" ", n)
