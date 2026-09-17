with open("docs/app.js", "r", encoding="utf-8") as f:
    text = f.read()

for line in text.splitlines():
    if "NewsletterModal" in line or "adminMail" in line or "publish" in line.lower():
        print(line)
