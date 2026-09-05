import re
with open('app/src/main/java/com/example/ui/screens/MainDashboardScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('"ВСЕГО ПО ПОДРАЗДЕЛЕНИЮ: $overallPositionsCount ПОЗ."', '"ВСЕГО: $overallPositionsCount ПОЗ."')

with open('app/src/main/java/com/example/ui/screens/MainDashboardScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
