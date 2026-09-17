with open("docs/index.html", "r", encoding="utf-8") as f:
    html = f.read()

# Verify link targets and update release notes on page
print("Occurrences of v3.4.4:", html.count("3.4.4"))
