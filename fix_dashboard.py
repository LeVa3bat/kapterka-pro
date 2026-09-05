import re

with open('app/src/main/java/com/example/ui/screens/MainDashboardScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if 'import com.example.util.AppTerminology' not in content:
    content = content.replace('import androidx.compose.ui.unit.dp', 'import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.platform.LocalContext\nimport com.example.util.AppTerminology')

# Add appMode resolving at the top of MainDashboardScreen
fun_start = """@Composable
fun MainDashboardScreen(
    profile: UserProfile?,
    stockRecords: List<StockRecord>,
    points: List<WarehousePoint>,"""

new_fun_start = """@Composable
fun MainDashboardScreen(
    profile: UserProfile?,
    stockRecords: List<StockRecord>,
    points: List<WarehousePoint>,"""
    
if fun_start in content:
    # Find where the function actually opens its body
    pass

# We will inject appMode fetching inside MainDashboardScreen
content = content.replace('val expandedPointIds = remember { mutableStateMapOf<String, Boolean>() }', 'val appMode = AppTerminology.getMode(LocalContext.current)\n    val expandedPointIds = remember { mutableStateMapOf<String, Boolean>() }')

# Replace hardcoded strings
content = content.replace('Text("Склад / Точка"', 'Text("База / Локация"')
content = content.replace('Text("Форма №8 (Расход)"', 'Text("Расход (Выдача)"')

with open('app/src/main/java/com/example/ui/screens/MainDashboardScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed Dashboard")
