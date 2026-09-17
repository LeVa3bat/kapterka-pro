with open("app/src/main/java/com/example/data/repository/KapterkaRepository.kt") as f:
    lines = f.readlines()
for i in range(185, 230):
    if i < len(lines):
        print(f"{i+1}: {lines[i]}", end="")
