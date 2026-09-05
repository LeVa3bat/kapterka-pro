with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix the end of baseStock block
target = "                )\n            }\n\n"
if target in content:
    content = content.replace(target, "                )\n            }\n            dao.insertStockRecords(baseStock)\n        }\n")

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed syntax")
