import re
import os

def modify_file(path, func):
    if not os.path.exists(path): return
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = func(content)
    if content != new_content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)

modify_file('app/src/main/java/com/example/ui/components/TacticalHeader.kt', lambda c:
    c.replace('import com.example.util.AppTerminology\n', '')
)

modify_file('app/src/main/java/com/example/ui/screens/MainDashboardScreen.kt', lambda c:
    c.replace('import com.example.util.AppTerminology\n', '')
     .replace('val appMode = AppTerminology.getMode(LocalContext.current)\n', '')
)

modify_file('app/src/main/java/com/example/ui/screens/AuthScreen.kt', lambda c:
    c.replace('import com.example.util.AppTerminology\n', '')
     .replace('import com.example.util.AppMode\n', '')
)

modify_file('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', lambda c:
    c.replace('com.example.data.local.InitialData.getDefaultCategories(com.example.util.AppTerminology.getMode(getApplication()))', 'com.example.data.local.InitialData.getDefaultCategories()')
)

with open('app/src/main/java/com/example/data/local/InitialData.kt', 'r', encoding='utf-8') as f:
    initial_content = f.read()
if "fun getDefaultCategories" not in initial_content:
    cats = """
    fun getDefaultCategories(): List<String> {
        return listOf("Служба РАВ", "Служба БПЛА и робототехники", "Служба связи и РЭБ", "Вещевая служба", "Медицинская служба", "Продовольственная служба", "ГСМ", "Автомобильная служба", "Инженерная служба", "Служба РХБЗ", "Трофеи", "Прочее")
    }
}"""
    initial_content = initial_content.replace('}\n}', cats)
    with open('app/src/main/java/com/example/data/local/InitialData.kt', 'w', encoding='utf-8') as f:
        f.write(initial_content)
