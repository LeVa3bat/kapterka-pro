with open('app/src/main/java/com/example/data/admin/FighterRegistryManager.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('"1-е Подразделение"', 'com.example.util.AppTerminology.getDefaultUnitName(com.example.util.AppMode.MILITARY)')
content = content.replace('"Подразделение"', '"Бригада / Подразделение"')

with open('app/src/main/java/com/example/data/admin/FighterRegistryManager.kt', 'w', encoding='utf-8') as f:
    f.write(content)
