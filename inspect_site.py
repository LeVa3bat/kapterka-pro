with open("docs/index.html", "r", encoding="utf-8") as f:
    text = f.read()

import re
print("Length of index.html:", len(text))

# Find buttons or sections
sections = re.findall(r'<section[^>]*id="([^"]+)"', text)
print("Sections found:", sections)

# Find any buttons
buttons = re.findall(r'<button[^>]*>(.*?)</button>', text, re.DOTALL)
print("Buttons found (sample):", [b.strip()[:40] for b in buttons[:10]])

# Search for any changelog or release block
for line in text.splitlines():
    if any(k in line.lower() for k in ["changelog", "история версий", "новости", "релиз", "обновлен"]):
        print("Line:", line.strip()[:100])
