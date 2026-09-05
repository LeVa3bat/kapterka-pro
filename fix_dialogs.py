import re
with open('app/src/main/java/com/example/ui/components/UnitKeySyncDialog.kt', 'r', encoding='utf-8') as f:
    content = f.read()
    
# We need AppMode in context. It's an AlertDialog.
if "val appMode =" not in content:
    content = content.replace("fun UnitKeySyncDialog(", "fun UnitKeySyncDialog(\n    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,\n    appMode: com.example.util.AppMode = com.example.util.AppTerminology.getMode(context),\n")

content = content.replace('"1-е Подразделение"', 'com.example.util.AppTerminology.getDefaultUnitName(appMode)')
content = content.replace('"Код подразделения"', 'com.example.util.AppTerminology.getUnitKeyLabel(appMode)')
content = content.replace('"Скопировать код подразделения"', '"Скопировать " + com.example.util.AppTerminology.getUnitKeyLabel(appMode).lowercase()')
content = content.replace('На другом телефоне при первом запуске нажать «Вход в подразделение»', 'На другом телефоне при первом запуске нажать «" + com.example.util.AppTerminology.getUnitSyncText(appMode) + "»')
content = content.replace('"ПОДКЛЮЧИТЬСЯ К ДРУГОМУ ПОДРАЗДЕЛЕНИЮ"', '"ПОДКЛЮЧИТЬСЯ К ДРУГОЙ БАЗЕ"')
content = content.replace('"Код подразделения $text скопирован в буфер"', '"$text скопирован в буфер"')
with open('app/src/main/java/com/example/ui/components/UnitKeySyncDialog.kt', 'w', encoding='utf-8') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/components/TacticalHeader.kt', 'r', encoding='utf-8') as f:
    content2 = f.read()
if "val appMode =" not in content2:
    content2 = content2.replace("fun TacticalHeader(", "fun TacticalHeader(\n    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,\n    appMode: com.example.util.AppMode = com.example.util.AppTerminology.getMode(context),\n")
content2 = content2.replace('"КОД ПОДРАЗДЕЛЕНИЯ / СКЛАДА"', 'com.example.util.AppTerminology.getUnitKeyLabel(appMode).uppercase()')
with open('app/src/main/java/com/example/ui/components/TacticalHeader.kt', 'w', encoding='utf-8') as f:
    f.write(content2)

with open('app/src/main/java/com/example/ui/components/ExcelReportPreviewDialog.kt', 'r', encoding='utf-8') as f:
    content3 = f.read()
if "val appMode =" not in content3:
    content3 = content3.replace("fun ExcelReportPreviewDialog(", "fun ExcelReportPreviewDialog(\n    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,\n    appMode: com.example.util.AppMode = com.example.util.AppTerminology.getMode(context),\n")
content3 = content3.replace('"Командир подразделения:"', '"Руководитель / Ответственный:"')
content3 = content3.replace('"РЕЕСТР ЗАЯВОК ПОДРАЗДЕЛЕНИЯ\\n"', '"РЕЕСТР ЗАЯВОК\\n"')
content3 = content3.replace('"Подразделение:\\t$unitName\\n\\n"', 'com.example.util.AppTerminology.getUnitNameLabel(appMode) + ":\\t$unitName\\n\\n"')
with open('app/src/main/java/com/example/ui/components/ExcelReportPreviewDialog.kt', 'w', encoding='utf-8') as f:
    f.write(content3)
    
