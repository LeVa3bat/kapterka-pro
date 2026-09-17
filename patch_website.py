with open("docs/index.html", "r", encoding="utf-8") as f:
    html = f.read()

# Replace version strings in HTML
html = html.replace("v=3.4.3.21", "v=3.4.4.22")
html = html.replace("v3.4.3", "v3.4.4")
html = html.replace("3.4.3 PRO", "3.4.4 PRO")
html = html.replace("kapterka-pro-v3.4.3-b21.apk", "kapterka-pro-v3.4.4-b22.apk")

with open("docs/index.html", "w", encoding="utf-8") as f:
    f.write(html)

print("docs/index.html successfully updated to v3.4.4 (b22)!")
