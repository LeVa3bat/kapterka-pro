import re

with open('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add getAppMode() helper to VM if not exists
if "private fun getAppMode()" not in content:
    helper = """
    private fun getAppMode(): com.example.util.AppMode {
        return com.example.util.AppTerminology.getMode(getApplication())
    }
"""
    content = content.replace("class KapterkaViewModel(application: Application) : AndroidViewModel(application) {", "class KapterkaViewModel(application: Application) : AndroidViewModel(application) {" + helper)

# Replace hardcoded "1-е Подразделение" with AppTerminology.getDefaultUnitName(getAppMode())
content = re.sub(r'"1-е Подразделение"', 'com.example.util.AppTerminology.getDefaultUnitName(getAppMode())', content)
content = re.sub(r'"Старшина подразделения"', 'com.example.util.AppTerminology.getAppRole(getAppMode())', content)
content = re.sub(r'"Старшина"', 'com.example.util.AppTerminology.getAppRole(getAppMode())', content)
content = content.replace('Подразделение: $resolvedUnitName', '${com.example.util.AppTerminology.getUnitNameLabel(getAppMode())}: $resolvedUnitName')
content = content.replace('Подключено к подразделению', 'Подключено к ${com.example.util.AppTerminology.getUnitNameLabel(getAppMode()).lowercase()}')
content = content.replace('Новый ключ подразделения:', 'Новый ${com.example.util.AppTerminology.getUnitKeyLabel(getAppMode()).lowercase()}:')
content = content.replace('База подразделения успешно синхронизирована!', 'База успешно синхронизирована!')
content = content.replace('Боец удален из реестра подразделений', 'Пользователь удален из реестра')

with open('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
