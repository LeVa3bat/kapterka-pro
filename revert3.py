import os
def modify_file(path, func):
    if not os.path.exists(path): return
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = func(content)
    if content != new_content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
            
modify_file('app/src/main/java/com/example/ui/components/UnitKeySyncDialog.kt', lambda c: 
    c.replace('fun UnitKeySyncDialog(\n    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,\n    appMode: com.example.util.AppMode = com.example.util.AppTerminology.getMode(context),\n', 'fun UnitKeySyncDialog(\n')
     .replace('com.example.util.AppTerminology.getDefaultUnitName(appMode)', '"1-е Подразделение"')
     .replace('com.example.util.AppTerminology.getUnitKeyLabel(appMode)', '"Код подразделения"')
     .replace('"Скопировать " + com.example.util.AppTerminology.getUnitKeyLabel(appMode).lowercase()', '"Скопировать код подразделения"')
     .replace('На другом телефоне при первом запуске нажать «" + com.example.util.AppTerminology.getUnitSyncText(appMode) + "»', 'На другом телефоне при первом запуске нажать «Вход в подразделение»')
     .replace('"ПОДКЛЮЧИТЬСЯ К ДРУГОЙ БАЗЕ"', '"ПОДКЛЮЧИТЬСЯ К ДРУГОМУ ПОДРАЗДЕЛЕНИЮ"')
)

modify_file('app/src/main/java/com/example/ui/components/TacticalHeader.kt', lambda c:
    c.replace('fun TacticalHeader(\n    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,\n    appMode: com.example.util.AppMode = com.example.util.AppTerminology.getMode(context),\n', 'fun TacticalHeader(\n')
     .replace('com.example.util.AppTerminology.getUnitKeyLabel(appMode).uppercase()', '"КОД ПОДРАЗДЕЛЕНИЯ / СКЛАДА"')
)

modify_file('app/src/main/java/com/example/ui/components/ExcelReportPreviewDialog.kt', lambda c:
    c.replace('fun ExcelReportPreviewDialog(\n    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,\n    appMode: com.example.util.AppMode = com.example.util.AppTerminology.getMode(context),\n', 'fun ExcelReportPreviewDialog(\n')
     .replace('"Руководитель / Ответственный:"', '"Командир подразделения:"')
     .replace('"РЕЕСТР ЗАЯВОК\\n"', '"РЕЕСТР ЗАЯВОК ПОДРАЗДЕЛЕНИЯ\\n"')
     .replace('com.example.util.AppTerminology.getUnitNameLabel(appMode) + ":\\t$unitName\\n\\n"', '"Подразделение:\\t$unitName\\n\\n"')
     .replace('"Подразделение" + ":\\t$unitName\\n\\n"', '"Подразделение:\\t$unitName\\n\\n"')
)

modify_file('app/src/main/java/com/example/ui/screens/RequestsScreen.kt', lambda c:
    c.replace('fun RequestsScreen(\n    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,\n    appMode: com.example.util.AppMode = com.example.util.AppTerminology.getMode(context),\n', 'fun RequestsScreen(\n')
     .replace('if (appMode == com.example.util.AppMode.MILITARY) "Медик" else "Имя заявителя"', '"Медик подразделения"')
)

modify_file('app/src/main/java/com/example/data/sync/FirebaseSyncManager.kt', lambda c:
    c.replace('Подключение к каналу', 'Подключение к каналу подразделения')
     .replace('удаленной базы', 'базы подразделения')
     .replace('База синхронизирована', 'База подразделения синхронизирована')
     .replace('База подразделения подразделения', 'База подразделения')
)

modify_file('app/src/main/java/com/example/util/TacticalNotificationHelper.kt', lambda c:
    c.replace('"Имущество выдано (${req.applicantName})"', '"Имущество выдано подразделению (${req.applicantName})"')
     .replace('"Учет синхронизирован"', '"Учет синхронизирован по подразделению"')
     .replace('"Учет синхронизирован по подразделению по подразделению"', '"Учет синхронизирован по подразделению"')
)

modify_file('app/src/main/java/com/example/data/admin/FighterRegistryManager.kt', lambda c:
    c.replace('com.example.util.AppTerminology.getDefaultUnitName(com.example.util.AppMode.MILITARY)', '"1-е Подразделение"')
     .replace('"Бригада / Подразделение"', '"Подразделение"')
)

modify_file('app/src/main/java/com/example/ui/screens/MoreSettingsScreen.kt', lambda c:
    c.replace('val appMode = remember { com.example.util.AppTerminology.getMode(context) }\n', '')
     .replace('com.example.util.AppTerminology.getUnitNameLabel(appMode) + ":"', '"Подразделение:"')
     .replace('"Введите название"', '"Название подразделения"')
     .replace('com.example.util.AppTerminology.getDefaultUnitName(appMode)', '"1-е Подразделение"')
     .replace('"Компактное меню управления"', '"Компактное меню управления подразделением и группами"')
     .replace('com.example.util.AppTerminology.getUnitNameLabel(appMode) + ": ${profile?.unitName ?: "', '"Подразделение: ${profile?.unitName ?: "')
     .replace('"Устройств в сети:"', '"Устройств в сети подразделения:"')
     .replace('в вашей структуре,', 'в вашем подразделении,')
     .replace('код', 'код склада/подразделения')
     .replace('"Выйти и сменить сферу деятельности"', '"Сменить позывной / Выйти из подразделения"')
     .replace('"Выйти / Сбросить профиль"', '"Сменить позывной / Выйти из подразделения"')
)
