with open("docs/index.html") as f:
    text = f.read()

import re
matches = re.findall(r'.{0,50}(?:v3\.4\.[0-9]|b[0-9]{2}|kapterka-pro[^\s"\'<>]*\.apk).{0,50}', text)
for m in matches[:15]:
    print(m)
