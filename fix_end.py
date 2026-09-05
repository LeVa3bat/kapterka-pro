with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

if lines[-1].strip() == '}':
    lines.pop()

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.writelines(lines)
print("Removed extra end bracket")
