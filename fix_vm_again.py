import re

with open('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('com.example.util.AppTerminology.getMode(application)', 'com.example.util.AppTerminology.getMode(getApplication())')

with open('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed VM Application context")
