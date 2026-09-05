with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    new_lines.append(line)
    if i == 113:  # line 114 (0-indexed 113)
        new_lines.append("            dao.insertStockRecords(baseStock)\n")
        new_lines.append("        }\n")
        new_lines.append("    }\n")

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
