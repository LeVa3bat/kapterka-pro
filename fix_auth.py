import re

with open('app/src/main/java/com/example/ui/screens/AuthScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports if missing
if 'import com.example.util.AppMode' not in content:
    content = content.replace('import java.util.UUID', 'import java.util.UUID\nimport com.example.util.AppMode\nimport com.example.util.AppTerminology\nimport androidx.compose.ui.platform.LocalContext')

# Add mode selection state
auth_fun_start = """@Composable
fun AuthScreen(
    currentProfile: UserProfile?,
    onCompleteAuth: (UserProfile) -> Unit
) {"""

new_auth_fun_start = """@Composable
fun AuthScreen(
    currentProfile: UserProfile?,
    onCompleteAuth: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    var isModeSelected by remember { mutableStateOf(AppTerminology.isModeSelected(context)) }
    
    if (!isModeSelected) {
        ModeSelectionScreen(onModeSelected = { mode ->
            AppTerminology.setMode(context, mode)
            isModeSelected = true
        })
        return
    }

    val appMode = AppTerminology.getMode(context)
"""

if auth_fun_start in content:
    content = content.replace(auth_fun_start, new_auth_fun_start)

# Add ModeSelectionScreen composable at the end
mode_screen = """
@Composable
fun ModeSelectionScreen(onModeSelected: (AppMode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Выберите вашу сферу",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TacticalTextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Интерфейс и база данных будут адаптированы под ваши задачи",
            fontSize = 14.sp,
            color = TacticalTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        ModeCard("🪖 Военное дело", "Спецучет, БК, Форма 18", AppMode.MILITARY, onModeSelected)
        Spacer(modifier = Modifier.height(16.dp))
        ModeCard("🏗 Строительство", "Инструмент, объекты, бригады", AppMode.CONSTRUCTION, onModeSelected)
        Spacer(modifier = Modifier.height(16.dp))
        ModeCard("📦 Склад и логистика", "Товары, упаковка, перемещения", AppMode.WAREHOUSE, onModeSelected)
        Spacer(modifier = Modifier.height(16.dp))
        ModeCard("⚙️ Базовый учет", "Универсальный учет инвентаря", AppMode.UNIVERSAL, onModeSelected)
    }
}

@Composable
fun ModeCard(title: String, desc: String, mode: AppMode, onClick: (AppMode) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(mode) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TacticalTextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = desc, fontSize = 13.sp, color = TacticalTextSecondary)
        }
    }
}
"""
content = content + mode_screen

# Replace hardcoded strings in AuthScreen
content = content.replace('Text("Позывной"', 'Text(AppTerminology.getCallsignLabel(appMode)')
content = content.replace('Text("Подразделение"', 'Text(AppTerminology.getUnitNameLabel(appMode)')
content = content.replace('Text("Ключ подразделения"', 'Text(AppTerminology.getUnitKeyLabel(appMode)')
content = content.replace('if (selectedTab == 0) "Зарегистрировать подразделение" else "Войти в подразделение"', 'if (selectedTab == 0) "Регистрация" else "Вход"')
content = content.replace('Text("Ваш личный позывной (имя)"', 'Text("Ваш личный идентификатор"')
content = content.replace('Text("Название вашего подразделения (роты, взвода)"', 'Text("Название вашей команды/базы"')
content = content.replace('Text("Введите общий ключ или нажмите 🔄"', 'Text("Общий ключ для синхронизации"')
content = content.replace('if (selectedTab == 0) "Зарегистрироваться и создать ключ" else "Войти в подразделение"', 'if (selectedTab == 0) "Зарегистрироваться и создать ключ" else "Войти"')

with open('app/src/main/java/com/example/ui/screens/AuthScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed AuthScreen")
