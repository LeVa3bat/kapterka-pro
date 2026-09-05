with open('app/src/main/java/com/example/util/TacticalNotificationHelper.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('"Имущество выдано подразделению (${req.applicantName})"', '"Имущество выдано (${req.applicantName})"')
content = content.replace('"Учет синхронизирован по подразделению"', '"Учет синхронизирован"')

with open('app/src/main/java/com/example/util/TacticalNotificationHelper.kt', 'w', encoding='utf-8') as f:
    f.write(content)
