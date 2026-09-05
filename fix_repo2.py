import re

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('suspend fun ensureInitialized() {', 'suspend fun ensureInitialized(appMode: com.example.util.AppMode = com.example.util.AppMode.MILITARY) {')

with open('app/src/main/java/com/example/data/repository/KapterkaRepository.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed Repo ensureInitialized")
