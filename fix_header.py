import re

with open('app/src/main/java/com/example/ui/components/TacticalHeader.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if 'import com.example.util.AppTerminology' not in content:
    content = content.replace('import androidx.compose.ui.unit.dp', 'import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.platform.LocalContext\nimport com.example.util.AppTerminology')

content = content.replace('val clipboardManager = LocalClipboardManager.current', 'val clipboardManager = LocalClipboardManager.current\n    val appMode = AppTerminology.getMode(LocalContext.current)')
content = content.replace('Text(\n                        text = "Подразделение",', 'Text(\n                        text = AppTerminology.getUnitNameLabel(appMode),')
content = content.replace('text = "Старшина"', 'text = AppTerminology.getCallsignLabel(appMode)')

with open('app/src/main/java/com/example/ui/components/TacticalHeader.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed Header")
