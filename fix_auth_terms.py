import re
with open('app/src/main/java/com/example/ui/screens/AuthScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# I need to ensure AppMode is available in AuthScreen context. It usually is via AppTerminology.getMode(context).
# Actually, the file probably already uses it. Let's see if we can just replace strings.
content = content.replace('"Воинский учет и снабжение подразделения"', 'if (appMode == com.example.util.AppMode.MILITARY) "Воинский учет и снабжение подразделения" else "Учет имущества и снабжение"')
content = content.replace('"1-е Подразделение"', 'com.example.util.AppTerminology.getDefaultUnitName(appMode)')
content = content.replace('"Вход в подразделение"', 'com.example.util.AppTerminology.getUnitSyncText(appMode)')
content = content.replace('Подразделение / Рота', '${com.example.util.AppTerminology.getUnitNameLabel(appMode)}')
content = content.replace('Введите подразделение (например: 1-я рота)', 'Введите название: ${com.example.util.AppTerminology.getDefaultUnitName(appMode)}')

with open('app/src/main/java/com/example/ui/screens/AuthScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
