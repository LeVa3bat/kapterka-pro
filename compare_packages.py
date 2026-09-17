import zipfile
import re

def get_dex_strings(apk_path):
    z = zipfile.ZipFile(apk_path)
    strings = set()
    for name in z.namelist():
        if name.endswith(".dex"):
            data = z.read(name)
            # Find class definitions like Lcom/something...;
            matches = re.findall(b'L([a-zA-Z0-9_/$]+);', data)
            for m in matches:
                strings.add(m.decode('latin1'))
    return strings

s21 = get_dex_strings("docs/kapterka-pro-v3.4.3-b21.apk")
s22 = get_dex_strings("docs/kapterka-pro-v3.4.4-b22.apk")

print("Classes in b21:", len(s21))
print("Classes in b22:", len(s22))

diff_22 = s22 - s21
print("Classes only in b22:", len(diff_22))

# Group diff by top package
from collections import Counter
counts = Counter([c.split('/')[0] + ('/' + c.split('/')[1] if '/' in c else '') for c in diff_22])
for prefix, count in counts.most_common(15):
    print(f"  {prefix}: {count} classes")
