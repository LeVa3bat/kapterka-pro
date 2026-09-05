import re

with open('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('repository.initialize()', 'repository.initialize(com.example.util.AppTerminology.getMode(application))')

with open('app/src/main/java/com/example/ui/viewmodel/KapterkaViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed VM Init")
