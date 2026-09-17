with open("docs/app.js", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i in range(1405, 1455):
    if i < len(lines):
        print(f"{i+1}: {lines[i]}", end="")
