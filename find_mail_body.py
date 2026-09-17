with open("docs/index.html", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i in range(1470, 1530):
    if i < len(lines):
        print(f"{i+1}: {lines[i]}", end="")
