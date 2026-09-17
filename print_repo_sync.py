with open("app/src/main/java/com/example/data/repository/KapterkaRepository.kt") as f:
    lines = f.readlines()
for i in range(110, 150):
    if i < len(lines):
        print(f"{i+1}: {lines[i]}", end="")
