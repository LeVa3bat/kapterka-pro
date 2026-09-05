with open('app/src/main/java/com/example/data/local/KapterkaDatabase.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('dao.insertPoints(InitialData.defaultPoints)', '// Points initialized in Repository')
content = content.replace('dao.insertItems(InitialData.defaultItems)', '// Items initialized in Repository')

with open('app/src/main/java/com/example/data/local/KapterkaDatabase.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed KapterkaDatabase")
