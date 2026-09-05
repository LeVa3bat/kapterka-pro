with open('app/src/main/java/com/example/ui/screens/RequestsScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if "val appMode =" not in content:
    content = content.replace("fun RequestsScreen(", "fun RequestsScreen(\n    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,\n    appMode: com.example.util.AppMode = com.example.util.AppTerminology.getMode(context),\n")

content = content.replace('"Медик подразделения"', 'if (appMode == com.example.util.AppMode.MILITARY) "Медик" else "Имя заявителя"')
with open('app/src/main/java/com/example/ui/screens/RequestsScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
