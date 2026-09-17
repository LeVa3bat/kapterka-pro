with open("docs/index.html", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i in range(1300, 1365):
    if i < len(lines):
        print(f"{i+1}: {lines[i]}", end="")
