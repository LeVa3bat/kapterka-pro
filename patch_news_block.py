with open("docs/index.html", "r", encoding="utf-8") as f:
    html = f.read()

# Let's inspect where changelog or news block is
import re
matches = [m.start() for m in re.finditer(r'3\.4\.', html)]
for idx in matches[:5]:
    print(html[idx-100:idx+200])
    print("="*40)
