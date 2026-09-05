with open('app/src/main/java/com/example/data/local/InitialData.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('"Основной склад подразделения"', '"Основной склад"')

with open('app/src/main/java/com/example/data/local/InitialData.kt', 'w', encoding='utf-8') as f:
    f.write(content)
