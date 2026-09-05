with open('app/src/main/java/com/example/ui/screens/MoreSettingsScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Make sure we have appMode in context
# In MoreSettingsScreen, it is inside a composable.
if "val appMode =" not in content:
    content = content.replace("fun MoreSettingsScreen(", "fun MoreSettingsScreen(\n")
    content = content.replace("    viewModel: KapterkaViewModel,", "    viewModel: KapterkaViewModel,\n")
    content = content.replace("    onNavigateToPointSettings: () -> Unit", "    onNavigateToPointSettings: () -> Unit\n")
    # Actually it's easier to just add it inside the composable body
    content = content.replace("    val context = LocalContext.current\n", "    val context = LocalContext.current\n    val appMode = remember { com.example.util.AppTerminology.getMode(context) }\n")

content = content.replace('"Подразделение:"', 'com.example.util.AppTerminology.getUnitNameLabel(appMode) + ":"')
content = content.replace('"Название подразделения"', '"Введите название"')
content = content.replace('"1-е Подразделение"', 'com.example.util.AppTerminology.getDefaultUnitName(appMode)')
content = content.replace('"Компактное меню управления подразделением и группами"', '"Компактное меню управления"')
content = content.replace('"Подразделение: ${profile?.unitName ?: "', 'com.example.util.AppTerminology.getUnitNameLabel(appMode) + ": ${profile?.unitName ?: "')
content = content.replace('"Устройств в сети подразделения:"', '"Устройств в сети:"')
content = content.replace('в вашем подразделении,', 'в вашей структуре,')
content = content.replace('код склада/подразделения', 'код')
content = content.replace('"Сменить позывной / Выйти из подразделения"', '"Выйти / Сбросить профиль"')

with open('app/src/main/java/com/example/ui/screens/MoreSettingsScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
