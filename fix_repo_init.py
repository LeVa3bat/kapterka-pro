import re

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "val currentPoints = dao.getAllPoints().first()"
new_code = """
        // Always try to insert default items to ensure updates like new ammo are present (ConflictStrategy is REPLACE/IGNORE)
        dao.insertItems(InitialData.getDefaultItems())

        val currentPoints = dao.getAllPoints().first()
"""

if "dao.insertItems(InitialData.getDefaultItems())" not in content[:content.find(target)]:
    content = content.replace(target, new_code)

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated init logic")
