with open("docs/index.html", "r", encoding="utf-8") as f:
    text = f.read()

import re
modals = re.findall(r'id="(modal[^"]+)"', text)
print("Modals found:", modals)

for line in text.splitlines():
    if "openNewsletterModal" in line:
        print("Line:", line.strip())
