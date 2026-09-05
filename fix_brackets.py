with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "            dao.insertOrUpdateStockList(baseStock)\n        }\n"
if target in content:
    content = content.replace(target, "            dao.insertOrUpdateStockList(baseStock)\n        }\n    }\n")

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed brackets")
